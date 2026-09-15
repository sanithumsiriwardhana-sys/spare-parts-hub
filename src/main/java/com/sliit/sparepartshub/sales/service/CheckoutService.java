package com.sliit.sparepartshub.sales.service;

import com.sliit.sparepartshub.entity.*;
import com.sliit.sparepartshub.sales.dto.CartItem;
import com.sliit.sparepartshub.sales.dto.CheckoutResult;
import com.sliit.sparepartshub.sales.dto.CompatibilityConflict;
import com.sliit.sparepartshub.sales.dto.PriceChangeRow;
import com.sliit.sparepartshub.sales.dto.SelectedSerial;
import com.sliit.sparepartshub.sales.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * UC-02 steps 9-14 (Complete Checkout). Everything from "revalidate the
 * cart" through "generate the pick ticket" happens inside one
 * @Transactional method - if anything fails partway through (stock
 * changed under us, a product disappeared, a selected serial got sold
 * elsewhere), nothing commits. That's what "atomic operation" in the use
 * case's postconditions actually requires: a half-saved sale with no
 * matching stock deduction (or a serial marked sold twice) would be
 * worse than no sale at all.
 */
@Service
public class CheckoutService {

    private final CartService cartService;
    private final ProductRepository productRepository;
    private final CompatibilityService compatibilityService;
    private final SerialNumberService serialNumberService;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PickTicketRepository pickTicketRepository;
    private final PickTicketItemRepository pickTicketItemRepository;

    public CheckoutService(CartService cartService,
                           ProductRepository productRepository,
                           CompatibilityService compatibilityService,
                           SerialNumberService serialNumberService,
                           SaleRepository saleRepository,
                           SaleItemRepository saleItemRepository,
                           PickTicketRepository pickTicketRepository,
                           PickTicketItemRepository pickTicketItemRepository) {
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.compatibilityService = compatibilityService;
        this.serialNumberService = serialNumberService;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.pickTicketRepository = pickTicketRepository;
        this.pickTicketItemRepository = pickTicketItemRepository;
    }

    @Transactional
    public CheckoutResult checkout(HttpSession session, User currentUser, String overrideReason,
                                   boolean confirmPriceChanges) {
        List<CartItem> cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            throw new CheckoutValidationException("Your cart is empty.");
        }

        // UC-02 step 10: revalidate stock against the DB as it actually
        // is right now, not what the cart last knew - another sale could
        // have happened since the item was added. Serial numbers get the
        // same treatment (see SerialNumberService.revalidateAtCheckout) -
        // both checks happen in this same first pass, before anything is
        // written, so a failure here leaves no partial state behind.
        List<Integer> productIds = cart.stream().map(CartItem::getProductId).collect(Collectors.toList());
        Map<Integer, Product> freshProducts = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        Map<Integer, List<SerialNumber>> validatedSerialsByProduct = new HashMap<>();
        List<PriceChangeRow> priceChanges = new ArrayList<>();

        for (CartItem item : cart) {
            Product fresh = freshProducts.get(item.getProductId());
            if (fresh == null) {
                throw new CheckoutValidationException(
                        "\"" + item.getName() + "\" is no longer available. Please remove it from the cart.");
            }
            if (fresh.getStockCount() < item.getQuantity()) {
                throw new CheckoutValidationException(
                        "Only " + fresh.getStockCount() + " unit(s) of \"" + fresh.getName()
                                + "\" left in stock - please adjust the quantity in your cart.");
            }

            // UC-02 extension 10a: only flag a genuine catalog price
            // drift - compare the DB's current price against what this
            // item's catalog price was when it was added
            // (originalPrice), not against the effective price it'll
            // actually be charged at. A line the Sales Executive has
            // already manually discounted on the spot (see
            // CartService.updatePrice) is a deliberate decision, already
            // made with a reason recorded where the policy required one
            // - re-flagging it here as an unexpected change would be
            // wrong and would block a completely normal discount from
            // ever completing checkout. Only items still at their
            // original catalog price get this check.
            boolean manuallyDiscounted = item.getPrice().compareTo(item.getOriginalPrice()) != 0;
            if (!manuallyDiscounted && fresh.getPrice().compareTo(item.getOriginalPrice()) != 0) {
                priceChanges.add(new PriceChangeRow(fresh.getProductId(), fresh.getName(),
                        item.getOriginalPrice(), fresh.getPrice()));
            }

            if (!item.getSelectedSerials().isEmpty()) {
                List<Integer> serialIds = item.getSelectedSerials().stream()
                        .map(SelectedSerial::getSerialId)
                        .collect(Collectors.toList());
                List<SerialNumber> validated = serialNumberService.revalidateAtCheckout(item.getProductId(), serialIds);
                validatedSerialsByProduct.put(item.getProductId(), validated);
            }
        }

        if (!priceChanges.isEmpty() && !confirmPriceChanges) {
            throw new PriceChangeConfirmationRequiredException(priceChanges);
        }

        // UC-02 step 6a re-checked against the FINAL cart - removals or
        // quantity edits since the last Add to Cart could have changed
        // the conflict set.
        List<Product> cartProducts = new ArrayList<>(freshProducts.values());
        List<CompatibilityConflict> conflicts = compatibilityService.checkCartConflicts(cartProducts);
        if (!conflicts.isEmpty() && (overrideReason == null || overrideReason.isBlank())) {
            throw new CompatibilityOverrideRequiredException(conflicts);
        }

        BigDecimal total = cart.stream()
                .map(i -> finalUnitPrice(i, freshProducts.get(i.getProductId()))
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Save first so the generated ID can seed the human-readable code.
        Sale sale = new Sale();
        sale.setSoldBy(currentUser);
        sale.setAmount(total);
        sale = saleRepository.save(sale);
        sale.setSaleCode("SALE-" + String.format("%06d", sale.getSaleId()));
        sale = saleRepository.save(sale);
        Sale savedSale = sale;

        Set<Integer> conflictedProductIds = conflicts.stream()
                .flatMap(c -> Stream.of(c.getProductAId(), c.getProductBId()))
                .collect(Collectors.toSet());

        for (CartItem item : cart) {
            Product product = freshProducts.get(item.getProductId());
            BigDecimal unitPrice = finalUnitPrice(item, product);

            SaleItem saleItem = new SaleItem();
            saleItem.setSale(savedSale);
            saleItem.setProduct(product);
            saleItem.setQuantity(item.getQuantity());
            // Captured at sale time, deliberately independent of
            // product.price - see the comment on SaleItem.priceAtSale.
            // For a manually-discounted line this is the negotiated
            // price the customer actually paid, not the catalog price.
            saleItem.setPriceAtSale(unitPrice);
            if (item.getDiscountReason() != null && !item.getDiscountReason().isBlank()) {
                saleItem.setDiscountReason(item.getDiscountReason());
            }
            if (conflictedProductIds.contains(product.getProductId())) {
                saleItem.setCompatibilityOverrideReason(overrideReason);
            }
            saleItemRepository.save(saleItem);

            // UC-02 step 12: atomic stock deduction, same transaction as
            // the sale itself.
            product.setStockCount(product.getStockCount() - item.getQuantity());
            productRepository.save(product);

            // UC-02 postcondition 3: link the specific units sold, if
            // this product is serial-tracked.
            List<SerialNumber> serials = validatedSerialsByProduct.get(item.getProductId());
            if (serials != null) {
                serialNumberService.markSold(serials, savedSale);
            }
        }

        // UC-02 step 14: generate the QR pick ticket for warehouse
        // fulfillment. See PickTicketRepository's comment - this table
        // conceptually belongs to Function 1 (inventory), which doesn't
        // exist as a package yet.
        PickTicket ticket = new PickTicket();
        ticket.setSale(savedSale);
        ticket.setStatus(PickTicket.Status.pending);
        ticket = pickTicketRepository.save(ticket);
        ticket.setTicketCode("PT-" + String.format("%06d", ticket.getTicketId()));
        ticket = pickTicketRepository.save(ticket);

        for (CartItem item : cart) {
            PickTicketItem pti = new PickTicketItem();
            pti.setTicket(ticket);
            pti.setProduct(freshProducts.get(item.getProductId()));
            pti.setQuantity(item.getQuantity());
            pickTicketItemRepository.save(pti);
        }

        cartService.clearCart(session);

        return new CheckoutResult(savedSale.getSaleId(), savedSale.getSaleCode(), ticket.getTicketCode(), total, cart.size());
    }

    // A cart line's actual charged price: the manually-negotiated price
    // if the Sales Executive discounted it on the spot (CartItem.price
    // != originalPrice - see CartService.updatePrice), otherwise the
    // fresh catalog price from the DB (which may have just been
    // confirmed as changed via confirmPriceChanges above). Never falls
    // back to CartItem.originalPrice directly - that's only ever used
    // as the comparison baseline for drift detection, not as a price
    // that's actually charged.
    private BigDecimal finalUnitPrice(CartItem item, Product fresh) {
        boolean manuallyDiscounted = item.getPrice().compareTo(item.getOriginalPrice()) != 0;
        return manuallyDiscounted ? item.getPrice() : fresh.getPrice();
    }
}