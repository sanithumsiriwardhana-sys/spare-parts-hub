package com.sliit.sparepartshub.sales.dto;

import java.math.BigDecimal;

/**
 * One product's all-time performance for the dashboard's Top Sellers
 * list - ranked by revenue, per the "products driving the most revenue"
 * framing, not just units moved (a cheap cable selling 50 units isn't
 * necessarily more important to the business than a GPU selling 3).
 */
public class TopProductRow {

    private final String productName;
    private final long unitsSold;
    private final BigDecimal revenue;

    public TopProductRow(String productName, long unitsSold, BigDecimal revenue) {
        this.productName = productName;
        this.unitsSold = unitsSold;
        this.revenue = revenue;
    }

    public String getProductName() {
        return productName;
    }

    public long getUnitsSold() {
        return unitsSold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
