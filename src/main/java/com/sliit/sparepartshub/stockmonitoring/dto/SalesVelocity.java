package com.sliit.sparepartshub.stockmonitoring.dto;

/**
 * Aggregate projection: total units sold for one product within a
 * lookback window. Populated via a JPQL constructor expression in
 * SaleItemRepository - not a JPA entity, just a query result shape.
 */
public class SalesVelocity {

    private final Integer productId;
    private final Long unitsSold;

    public SalesVelocity(Integer productId, Long unitsSold) {
        this.productId = productId;
        this.unitsSold = unitsSold;
    }

    public Integer getProductId() {
        return productId;
    }

    public Long getUnitsSold() {
        return unitsSold;
    }
}
