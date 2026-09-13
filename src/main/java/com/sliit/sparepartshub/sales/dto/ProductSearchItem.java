package com.sliit.sparepartshub.sales.dto;

import java.math.BigDecimal;

/**
 * Flat view of a Product for the instant-search JSON endpoint. Product
 * itself has a LAZY @ManyToOne to StorageLocation - serializing the
 * entity directly risks a LazyInitializationException outside the
 * transaction, so the search API returns this instead.
 */
public class ProductSearchItem {

    private final Integer productId;
    private final String name;
    private final String category;
    private final String brand;
    private final BigDecimal price;
    private final Integer stockCount;

    public ProductSearchItem(Integer productId, String name, String category,
                              String brand, BigDecimal price, Integer stockCount) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.price = price;
        this.stockCount = stockCount;
    }

    public Integer getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStockCount() {
        return stockCount;
    }
}
