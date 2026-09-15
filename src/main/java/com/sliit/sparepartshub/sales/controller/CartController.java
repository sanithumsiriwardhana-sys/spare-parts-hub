package com.sliit.sparepartshub.sales.controller;

import com.sliit.sparepartshub.sales.dto.CartItem;
import com.sliit.sparepartshub.sales.dto.CartResponse;
import com.sliit.sparepartshub.sales.dto.CompatibilityConflict;
import com.sliit.sparepartshub.sales.service.CartService;
import com.sliit.sparepartshub.sales.service.InsufficientStockException;
import com.sliit.sparepartshub.sales.service.SerialSelectionException;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Session-cart endpoints for Function 2 (UC-02 steps 4-8). Split out
 * from SalesController now that cart logic has grown past "one
 * controller handles it all" - matches the "one controller per distinct
 * concern" convention in PROJECT_SUMMARY.md. Falls under the same
 * /sales/** route prefix, so no SecurityConfig change is needed - it's
 * already restricted to SALES_EXEC/ADMIN.
 */
@RestController
@RequestMapping("/sales/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse view(HttpSession session) {
        return toResponse(cartService.getCart(session), List.of());
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestParam Integer productId,
                                  @RequestParam(defaultValue = "1") int quantity,
                                  @RequestParam(required = false) List<Integer> serialIds,
                                  HttpSession session) {
        try {
            List<CompatibilityConflict> conflicts = cartService.addItem(session, productId, quantity, serialIds);
            return ResponseEntity.ok(toResponse(cartService.getCart(session), conflicts));
        } catch (InsufficientStockException | IllegalArgumentException | SerialSelectionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Anything unexpected - the front-end's fetch() handlers
            // only know how to parse a JSON body, so this must never
            // fall through to a raw 500/HTML error page.
            log.error("Failed to add item {} to cart", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Something went wrong adding that item. Please try again."));
        }
    }

    // Sales Executive manually lowering a line's price on the spot -
    // see CartService.updatePrice for the discount rules. Kept as its
    // own endpoint rather than folded into add(): it edits an existing
    // line rather than adding a new one.
    @PostMapping("/price")
    public ResponseEntity<?> updatePrice(@RequestParam Integer productId,
                                          @RequestParam BigDecimal price,
                                          @RequestParam(required = false) String reason,
                                          HttpSession session) {
        try {
            cartService.updatePrice(session, productId, price, reason);
            return ResponseEntity.ok(toResponse(cartService.getCart(session), List.of()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update price for cart item {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Something went wrong updating the price. Please try again."));
        }
    }

    @PostMapping("/remove")
    public CartResponse remove(@RequestParam Integer productId, HttpSession session) {
        List<CartItem> cart = cartService.removeItem(session, productId);
        return toResponse(cart, List.of());
    }

    private CartResponse toResponse(List<CartItem> cart, List<CompatibilityConflict> conflicts) {
        return new CartResponse(cart, cartService.cartTotal(cart), cartService.cartItemCount(cart), conflicts);
    }
}
