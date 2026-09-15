package com.sliit.sparepartshub.sales.controller;

import com.sliit.sparepartshub.entity.Sale;
import com.sliit.sparepartshub.entity.SaleItem;
import com.sliit.sparepartshub.entity.SerialNumber;
import com.sliit.sparepartshub.sales.dto.CheckoutResult;
import com.sliit.sparepartshub.sales.dto.ReceiptLineItem;
import com.sliit.sparepartshub.sales.dto.SaleHistoryRow;
import com.sliit.sparepartshub.sales.repository.SaleItemRepository;
import com.sliit.sparepartshub.sales.repository.SaleRepository;
import com.sliit.sparepartshub.sales.repository.SerialNumberRepository;
import com.sliit.sparepartshub.sales.service.CheckoutService;
import com.sliit.sparepartshub.sales.service.CheckoutValidationException;
import com.sliit.sparepartshub.sales.service.CompatibilityOverrideRequiredException;
import com.sliit.sparepartshub.sales.service.PriceChangeConfirmationRequiredException;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UC-02 steps 9-14 (Complete Checkout + receipt). Split from
 * SalesController/CartController per "one controller per distinct
 * concern".
 *
 * Note on pick tickets: checkout() still triggers PickTicket/
 * PickTicketItem creation (see CheckoutService - UC-02 step 14 requires
 * it). This controller deliberately does NOT fetch or expose ticket
 * code/QR data to the receipt page - that's internal warehouse
 * information, not something that belongs on a customer-facing receipt.
 * Function 1 (inventory) owns displaying it on their own screen.
 */
@Controller
@RequestMapping("/sales")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    // e.g. "24 Aug 2026, 10:41 AM" - shared by both the history list and
    // the receipt page so they read consistently.
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final CheckoutService checkoutService;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SerialNumberRepository serialNumberRepository;

    public CheckoutController(CheckoutService checkoutService,
                               SaleRepository saleRepository,
                               SaleItemRepository saleItemRepository,
                               SerialNumberRepository serialNumberRepository) {
        this.checkoutService = checkoutService;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.serialNumberRepository = serialNumberRepository;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestParam(required = false) String overrideReason,
                                       @RequestParam(defaultValue = "false") boolean confirmPriceChanges,
                                       HttpSession session,
                                       @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            CheckoutResult result = checkoutService.checkout(session, principal.getUser(), overrideReason, confirmPriceChanges);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "saleId", result.getSaleId(),
                    "saleCode", result.getSaleCode(),
                    "redirectUrl", "/sales/receipt/" + result.getSaleId()
            ));
        } catch (PriceChangeConfirmationRequiredException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "needsPriceConfirmation", true,
                    "priceChanges", e.getPriceChanges()
            ));
        } catch (CompatibilityOverrideRequiredException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "needsOverrideReason", true,
                    "conflicts", e.getConflicts()
            ));
        } catch (CheckoutValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            // Anything unexpected (DB hiccup, a bug) - the checkout JS
            // only knows how to handle a JSON body, so this must never
            // fall through to Spring's default HTML error page. No
            // sale is left half-committed either way: CheckoutService's
            // whole checkout() body is one @Transactional block, so a
            // failure here means nothing was saved.
            log.error("Checkout failed unexpectedly", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Something went wrong while completing checkout. Please try again."
            ));
        }
    }

    @GetMapping("/history")
    public String history(Model model) {
        try {
            List<Sale> sales = saleRepository.findTop50ByOrderBySoldAtDesc();
            List<SaleHistoryRow> rows = sales.stream()
                    .map(s -> new SaleHistoryRow(
                            s.getSaleId(), s.getSaleCode(),
                            s.getSoldAt() != null ? s.getSoldAt().format(DATE_FORMAT) : "-",
                            s.getSoldBy().getName(), s.getAmount()))
                    .collect(Collectors.toList());
            model.addAttribute("rows", rows);
            return "sales/history";
        } catch (Exception e) {
            log.error("Failed to load sales history", e);
            model.addAttribute("message",
                    "Sales history could not be loaded right now. Please try again shortly.");
            return "sales/error";
        }
    }

    @GetMapping("/receipt/{saleId}")
    public String receipt(@PathVariable Integer saleId, Model model) {
        try {
            Sale sale = saleRepository.findById(saleId)
                    .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + saleId));

            List<SaleItem> saleItems = saleItemRepository.findBySale_SaleId(saleId);

            // Serial numbers link to Sale, not SaleItem directly (schema has
            // no sale_item_id on serial_number) - group by product instead.
            // Safe because checkout merges quantities into one CartItem per
            // product, so a sale never has two SaleItem rows for the same
            // product to disambiguate between.
            List<SerialNumber> soldSerials = serialNumberRepository.findBySale_SaleId(saleId);
            Map<Integer, List<String>> serialValuesByProduct = soldSerials.stream()
                    .collect(Collectors.groupingBy(
                            sn -> sn.getProduct().getProductId(),
                            Collectors.mapping(SerialNumber::getSerialValue, Collectors.toList())));

            List<ReceiptLineItem> lineItems = saleItems.stream()
                    .map(si -> new ReceiptLineItem(
                            si.getProduct().getName(), si.getQuantity(), si.getPriceAtSale(),
                            si.getCompatibilityOverrideReason(), si.getDiscountReason(),
                            serialValuesByProduct.getOrDefault(si.getProduct().getProductId(), List.of())))
                    .collect(Collectors.toList());

            model.addAttribute("sale", sale);
            model.addAttribute("formattedDate", sale.getSoldAt() != null ? sale.getSoldAt().format(DATE_FORMAT) : "-");
            model.addAttribute("lineItems", lineItems);

            return "sales/receipt";
        } catch (IllegalArgumentException e) {
            // Distinct from the catch-all below: this is a bad/expired
            // link (e.g. saleId doesn't exist), not a server problem -
            // worth a clearer message since it's the more likely case
            // in practice.
            model.addAttribute("message",
                    "That sale could not be found - it may have been removed, or the link may be incorrect.");
            return "sales/error";
        } catch (Exception e) {
            log.error("Failed to load receipt for sale {}", saleId, e);
            model.addAttribute("message",
                    "This receipt could not be loaded right now. Please try again shortly.");
            return "sales/error";
        }
    }
}
