package com.sliit.sparepartshub.sales.dto;

import java.util.List;

/**
 * Distinct category/brand values available right now, for populating the
 * filter dropdowns on the product search screen. Not persisted, just a
 * query result shape - matches the pattern in
 * stockmonitoring/dto/CategoryStockSummary.java.
 */
public class SearchFilterOptions {

    private final List<String> categories;
    private final List<String> brands;

    public SearchFilterOptions(List<String> categories, List<String> brands) {
        this.categories = categories;
        this.brands = brands;
    }

    public List<String> getCategories() {
        return categories;
    }

    public List<String> getBrands() {
        return brands;
    }
}
