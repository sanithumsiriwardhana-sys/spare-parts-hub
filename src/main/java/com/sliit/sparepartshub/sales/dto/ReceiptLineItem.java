package com.sliit.sparepartshub.sales.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * One line on the printed receipt. Built from SaleItem in
 * CheckoutController rather than exposing the entity straight to the
 * template - keeps subtotal math out of Thymeleaf/SpEL and avoids
 * touching the shared entity classes just to add a convenience getter.
 */
public class ReceiptLineItem {

    private final String productName;
    private final int quantity;
    private final BigDecimal priceAtSale;
    private final BigDecimal subtotal;
    private final String compatibilityOverrideReason;
    private final String discountReason;
    private final List<String> serialValues; // empty if not serial-tracked

    public ReceiptLineItem(String productName, int quantity, BigDecimal priceAtSale,
                           String compatibilityOverrideReason, String discountReason,
                           List<String> serialValues) {
        this.productName = productName;
        this.quantity = quantity;
        this.priceAtSale = priceAtSale;
        this.subtotal = priceAtSale.multiply(BigDecimal.valueOf(quantity));
        this.compatibilityOverrideReason = compatibilityOverrideReason;
        this.discountReason = discountReason;
        this.serialValues = serialValues;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPriceAtSale() {
        return priceAtSale;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public String getCompatibilityOverrideReason() {
        return compatibilityOverrideReason;
    }

    public String getDiscountReason() {
        return discountReason;
    }

    public List<String> getSerialValues() {
        return serialValues;
    }
}