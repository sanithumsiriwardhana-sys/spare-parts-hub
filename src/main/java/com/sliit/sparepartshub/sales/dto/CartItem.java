package com.sliit.sparepartshub.sales.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * One line in the session-based cart. Mutable (quantity can be bumped in
 * place when the same product is added twice) - unlike the other DTOs in
 * this package this isn't a query projection, it's session state, so
 * that's an intentional deviation from "DTOs are immutable view models".
 *
 * Tracks two prices deliberately:
 *   - originalPrice: the catalog price at the moment this item was
 *     added, and never changes after that. This is the baseline
 *     CheckoutService compares against the DB's current price to detect
 *     a genuine catalog price drift (UC-02 extension 10a).
 *   - price: the effective price this line will actually be charged at.
 *     Starts out equal to originalPrice and only diverges if the Sales
 *     Executive manually discounts the line on the spot (see
 *     CartService.updatePrice) - a deliberate, already-confirmed
 *     decision, not something checkout should re-flag as an unexpected
 *     price change.
 */
public class CartItem {

    private final Integer productId;
    private final String name;
    private final String brand;
    private final String category;
    private final BigDecimal originalPrice;
    private BigDecimal price;
    private int quantity;

    // Non-null only while price is discounted below originalPrice - see
    // CartService.updatePrice for when a reason is required vs optional.
    // Carried through to SaleItem.discountReason at checkout.
    private String discountReason;

    // Empty for non-serial-tracked products. When non-empty, its size
    // must always equal quantity - enforced by CartService, not here.
    private final List<SelectedSerial> selectedSerials = new ArrayList<>();

    public CartItem(Integer productId, String name, String brand, String category,
                    BigDecimal price, int quantity) {
        this.productId = productId;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.originalPrice = price;
        this.price = price;
        this.quantity = quantity;
    }

    public Integer getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDiscountReason() {
        return discountReason;
    }

    public void setDiscountReason(String discountReason) {
        this.discountReason = discountReason;
    }

    public List<SelectedSerial> getSelectedSerials() {
        return selectedSerials;
    }

    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}