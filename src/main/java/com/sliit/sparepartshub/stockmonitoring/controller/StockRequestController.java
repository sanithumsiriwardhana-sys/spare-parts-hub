package com.sliit.sparepartshub.stockmonitoring.controller;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.StockRequest;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import com.sliit.sparepartshub.stockmonitoring.repository.ProductRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.StockRequestRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Function 3's demand-tracking sub-function (PBI-11): logs a customer's
 * interest in an out-of-stock item so staff can notify them once it's
 * back. Separate from StockMonitoringController since it's a distinct
 * page/concern, not part of the urgency dashboard itself.
 */
@Controller
public class StockRequestController {

    private final StockRequestRepository stockRequestRepository;
    private final ProductRepository productRepository;

    public StockRequestController(StockRequestRepository stockRequestRepository,
                                   ProductRepository productRepository) {
        this.stockRequestRepository = stockRequestRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/stockmonitoring/stock-requests")
    public String list(Model model) {
        List<StockRequest> pending = stockRequestRepository.findByStatus(StockRequest.Status.pending);
        List<Product> products = productRepository.findAll();

        model.addAttribute("pendingRequests", pending);
        model.addAttribute("products", products);
        return "stockmonitoring/stock-requests";
    }

    @PostMapping("/stockmonitoring/stock-requests")
    public String create(@RequestParam Integer productId,
                          @RequestParam String customerName,
                          @RequestParam(required = false) String customerEmail,
                          @RequestParam(required = false) String customerPhonenum,
                          @AuthenticationPrincipal CustomUserPrincipal principal) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product id: " + productId));

        StockRequest request = new StockRequest();
        request.setProduct(product);
        request.setLoggedBy(principal.getUser());
        request.setCustomerName(customerName);
        request.setCustomerEmail(customerEmail);
        request.setCustomerPhonenum(customerPhonenum);
        request.setStatus(StockRequest.Status.pending);

        stockRequestRepository.save(request);

        return "redirect:/stockmonitoring/stock-requests";
    }
}
