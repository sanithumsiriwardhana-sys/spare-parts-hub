package com.sliit.sparepartshub.stockmonitoring.dto;

/**
 * Aggregate projection: total stock_count across all products in one
 * category. Populated via a JPQL constructor expression in
 * ProductRepository - not a JPA entity, just a query result shape.
 */
public class CategoryStockSummary {

    private final String category;
    private final Long totalStock;

    public CategoryStockSummary(String category, Long totalStock) {
        this.category = category;
        this.totalStock = totalStock;
    }

    public String getCategory() {
        return category;
    }

    public Long getTotalStock() {
        return totalStock;
    }
}
