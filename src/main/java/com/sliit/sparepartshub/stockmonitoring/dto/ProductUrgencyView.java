package com.sliit.sparepartshub.stockmonitoring.dto;

import com.sliit.sparepartshub.entity.Product;

/**
 * Pairs a Product with its classified UrgencyLevel for the dashboard
 * table - keeps the classification call (which needs the service) out
 * of the Thymeleaf template.
 */
public class ProductUrgencyView {

    private final Product product;
    private final UrgencyLevel level;

    public ProductUrgencyView(Product product, UrgencyLevel level) {
        this.product = product;
        this.level = level;
    }

    public Product getProduct() {
        return product;
    }

    public UrgencyLevel getLevel() {
        return level;
    }
}
