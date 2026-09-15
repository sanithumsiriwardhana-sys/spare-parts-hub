package com.sliit.sparepartshub.sales.service;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.SerialNumber;
import com.sliit.sparepartshub.sales.dto.CartItem;
import com.sliit.sparepartshub.sales.dto.CompatibilityConflict;
import com.sliit.sparepartshub.sales.dto.SelectedSerial;
import com.sliit.sparepartshub.sales.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The cart lives in the HTTP session, not a database table. It's
 * scratch state until Complete Checkout turns it into a real Sale/
 * SaleItem - there's no cart table in schema.sql, and there doesn't need
 * to be one: a session naturally models "one cashier terminal, one
 * in-progress sale", and nothing needs to survive past checkout or
 * logout.
 */
@Service
public class CartService {

    private static final String SESSION_KEY = "salesCart";

    private final ProductRepository productRepository;
    private final CompatibilityService compatibilityService;
    private final SerialNumberService serialNumberService;

    public CartService(ProductRepository productRepository,
                       CompatibilityService compatibilityService,
                       SerialNumberService serialNumberService) {
        this.productRepository = productRepository;
        this.compatibilityService = compatibilityService;
        this.serialNumberService = serialNumberService;
    }

    @SuppressWarnings("unchecked")
    public List<CartItem> getCart(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute(SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(SESSION_KEY, cart);
        }
        return cart;
    }

    // Returns any compatibility conflicts the new item creates against
    // what's already in the cart (UC-02 step 6/7). Conflicts are a
    // warning, not a hard block - matches extension 6a, where the Sales
    // Executive can still proceed with an authorized override at
    // checkout.
    //
    // selectedSerialIds is required (exactly `quantity` of them) only if
    // the product turns out to be serial-tracked - see
    // SerialNumberService's class comment for how that's determined.
    // Non-serialized products can pass null/empty and it's ignored.
    public List<CompatibilityConflict> addItem(HttpSession session, Integer productId, int quantity,
                                               List<Integer> selectedSerialIds) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<CartItem> cart = getCart(session);

        int alreadyInCart = cart.stream()
                .filter(i -> i.getProductId().equals(productId))
                .mapToInt(CartItem::getQuantity)
                .sum();

        if (alreadyInCart + quantity > product.getStockCount()) {
            throw new InsufficientStockException(product.getName(), product.getStockCount(), alreadyInCart);
        }

        // Serial numbers already claimed elsewhere in this cart can't be
        // selected again, even for a different line - each row is one
        // physical unit.
        Set<Integer> alreadyReservedSerialIds = cart.stream()
                .flatMap(i -> i.getSelectedSerials().stream())
                .map(SelectedSerial::getSerialId)
                .collect(Collectors.toSet());

        List<SerialNumber> validatedSerials = serialNumberService.validateSelection(
                productId, selectedSerialIds, quantity, alreadyReservedSerialIds);

        List<Integer> existingProductIds = cart.stream().map(CartItem::getProductId).collect(Collectors.toList());
        List<Product> existingCartProducts = productRepository.findAllById(existingProductIds);

        List<CompatibilityConflict> conflicts = compatibilityService.checkAgainstCart(product, existingCartProducts);

        List<SelectedSerial> newSelectedSerials = validatedSerials.stream()
                .map(sn -> new SelectedSerial(sn.getSerialId(), sn.getSerialValue()))
                .collect(Collectors.toList());

        Optional<CartItem> existing = cart.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
            existing.get().getSelectedSerials().addAll(newSelectedSerials);
        } else {
            CartItem item = new CartItem(product.getProductId(), product.getName(), product.getBrand(),
                    product.getCategory(), product.getPrice(), quantity);
            item.getSelectedSerials().addAll(newSelectedSerials);
            cart.add(item);
        }

        return conflicts;
    }

    // Lets a Sales Executive lower an already-added line's price for
    // this sale only - the common "let me knock a bit off to close the
    // deal" negotiation. Deliberately a separate action from addItem,
    // not a parameter on it: it's editing an existing decision, not
    // making a new one.
    //
    // Only discounts are allowed (never a markup above the catalog
    // price) - CheckoutService relies on that to tell a genuine catalog
    // price change apart from a deliberate on-the-spot discount (see its
    // class comment). A reason is always optional; if one is given it's
    // carried through to SaleItem.discountReason for the audit trail,
    // but nothing here requires it.
    public void updatePrice(HttpSession session, Integer productId, BigDecimal newPrice, String reason) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }

        CartItem item = getCart(session).stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("That item is not in the cart."));

        if (newPrice.compareTo(item.getOriginalPrice()) > 0) {
            throw new IllegalArgumentException(
                    "Price cannot be raised above the catalog price of LKR " + item.getOriginalPrice() + ".");
        }

        boolean isDiscount = newPrice.compareTo(item.getOriginalPrice()) < 0;
        String trimmedReason = (reason == null || reason.isBlank()) ? null : reason.trim();

        item.setPrice(newPrice);
        // Only keep a reason attached while the line is actually
        // discounted - if the price is set back to the catalog price,
        // any earlier reason no longer describes anything real.
        item.setDiscountReason(isDiscount ? trimmedReason : null);
    }

    public List<CartItem> removeItem(HttpSession session, Integer productId) {
        List<CartItem> cart = getCart(session);
        cart.removeIf(i -> i.getProductId().equals(productId));
        return cart;
    }

    public BigDecimal cartTotal(List<CartItem> cart) {
        return cart.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int cartItemCount(List<CartItem> cart) {
        return cart.stream().mapToInt(CartItem::getQuantity).sum();
    }

    // Called by CheckoutService once a sale is successfully persisted -
    // the cart's job is done at that point, and leaving stale items
    // in-session would let the same items appear to be "in cart" again
    // after a completed sale.
    public void clearCart(HttpSession session) {
        getCart(session).clear();
    }
}