package com.sliit.sparepartshub.stockmonitoring.controller;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.StockRequest;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import com.sliit.sparepartshub.stockmonitoring.repository.ProductRepository;
import com.sliit.sparepartshub.stockmonitoring.service.StockRequestService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Function 3's demand-tracking sub-function (PBI-11): logs a customer's
 * interest in an out-of-stock item, notifies them once it's back, and
 * marks the request fulfilled once they buy it. Business logic (the
 * pending -> notified -> fulfilled rules) lives in StockRequestService,
 * not here - this controller only translates HTTP into service calls.
 */
@Controller
public class StockRequestController {

    private final StockRequestService stockRequestService;
    private final ProductRepository productRepository;

    public StockRequestController(StockRequestService stockRequestService,
                                  ProductRepository productRepository) {
        this.stockRequestService = stockRequestService;
        this.productRepository = productRepository;
    }

    @GetMapping("/stockmonitoring/stock-requests")
    public String list(Model model) {
        List<StockRequest> pending = stockRequestService.getPending();
        List<StockRequest> notified = stockRequestService.getNotified();
        List<Product> products = productRepository.findAll();

        model.addAttribute("pendingRequests", pending);
        model.addAttribute("notifiedRequests", notified);
        model.addAttribute("products", products);
        return "stockmonitoring/stock-requests";
    }

    @PostMapping("/stockmonitoring/stock-requests")
    public String create(@RequestParam Integer productId,
                         @RequestParam String customerName,
                         @RequestParam(required = false) String customerEmail,
                         @RequestParam(required = false) String customerPhonenum,
                         @AuthenticationPrincipal CustomUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product id: " + productId));

        stockRequestService.create(product, principal.getUser(), customerName, customerEmail, customerPhonenum);
        redirectAttributes.addFlashAttribute("message", "Stock request logged.");
        return "redirect:/stockmonitoring/stock-requests";
    }

    @PostMapping("/stockmonitoring/stock-requests/{id}/notify")
    public String notify(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            stockRequestService.markNotified(id);
            redirectAttributes.addFlashAttribute("message", "Customer marked as notified.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stockmonitoring/stock-requests";
    }

    @PostMapping("/stockmonitoring/stock-requests/{id}/fulfill")
    public String fulfill(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            stockRequestService.markFulfilled(id);
            redirectAttributes.addFlashAttribute("message", "Request marked as fulfilled.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stockmonitoring/stock-requests";
    }
}
