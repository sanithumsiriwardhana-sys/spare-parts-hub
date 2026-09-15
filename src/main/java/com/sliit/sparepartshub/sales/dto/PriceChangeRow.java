package com.sliit.sparepartshub.sales.dto;

import java.math.BigDecimal;

/**
 * One line in a price-change confirmation prompt (UC-02 extension 10a).
 * Built during checkout revalidation when a cart item's price no longer
 * matches what's currently in the DB - oldPrice is what the item was
 * added to the cart at, newPrice is the fresh value the sale will
 * actually be charged at if the cashier confirms.
 */
public class PriceChangeRow {

    private final Integer productId;
    private final String productName;
    private final BigDecimal oldPrice;
    private final BigDecimal newPrice;

    public PriceChangeRow(Integer productId, String productName, BigDecimal oldPrice, BigDecimal newPrice) {
        this.productId = productId;
        this.productName = productName;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
    }

    public Integer getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getOldPrice() {
        return oldPrice;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }
}
