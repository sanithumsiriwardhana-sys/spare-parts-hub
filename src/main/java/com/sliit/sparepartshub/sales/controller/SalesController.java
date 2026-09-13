package com.sliit.sparepartshub.sales.controller;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.sales.dto.AvailableSerialRow;
import com.sliit.sparepartshub.sales.dto.CartItem;
import com.sliit.sparepartshub.sales.dto.ProductSearchItem;
import com.sliit.sparepartshub.sales.dto.SearchFilterOptions;
import com.sliit.sparepartshub.sales.service.CartService;
import com.sliit.sparepartshub.sales.service.ProductSearchService;
import com.sliit.sparepartshub.sales.service.SerialNumberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Function 2 - Real-Time Product Search & Automated Checkout
 * Actor: Senior Sales Executive
 * Covers: PBI-05 to PBI-08 (Sprint 1)
 *
 * Handles the search screen and the serial-number picker lookup. Cart
 * mutation lives in CartController; checkout/receipt in
 * CheckoutController.
 */
@Controller
public class SalesController {

    private final ProductSearchService productSearchService;
    private final CartService cartService;
    private final SerialNumberService serialNumberService;

    public SalesController(ProductSearchService productSearchService, CartService cartService,
                            SerialNumberService serialNumberService) {
        this.productSearchService = productSearchService;
        this.cartService = cartService;
        this.serialNumberService = serialNumberService;
    }

    // Full page load - server-rendered so /sales works with JS disabled
    // and is a valid bookmarkable/shareable link with filters in the URL.
    // Also renders the current session cart, so a page reload doesn't
    // appear to have wiped out items the Sales Executive already added.
    @GetMapping("/sales")
    public String index(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            HttpSession session,
            Model model) {

        List<Product> results = productSearchService.search(keyword, category, brand);
        SearchFilterOptions filterOptions = productSearchService.getFilterOptions();
        List<CartItem> cart = cartService.getCart(session);

        model.addAttribute("results", results);
        model.addAttribute("filterOptions", filterOptions);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("category", category == null ? "" : category);
        model.addAttribute("brand", brand == null ? "" : brand);
        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartService.cartTotal(cart));
        model.addAttribute("cartItemCount", cartService.cartItemCount(cart));

        return "sales/index";
    }

    // Instant filtering for the category tabs and search-as-you-type on
    // the already-loaded page - the front-end calls this instead of
    // reloading /sales. JSON only, no brand param yet since the tabs UI
    // only filters by category/keyword for now.
    @GetMapping("/sales/api/search")
    @ResponseBody
    public List<ProductSearchItem> apiSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return productSearchService.searchAsDto(keyword, category, null);
    }

    // Powers the serial-number picker modal on Add to Cart. Empty list
    // means "this product isn't serial-tracked" - the front-end treats
    // that as "add normally, no picker needed" rather than an error.
    @GetMapping("/sales/products/{productId}/serials")
    @ResponseBody
    public List<AvailableSerialRow> availableSerials(@PathVariable Integer productId) {
        return serialNumberService.getAvailableSerials(productId);
    }
}
