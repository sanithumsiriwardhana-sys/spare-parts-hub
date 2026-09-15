package com.sliit.sparepartshub.sales.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything the Sales dashboard widget needs, computed in one place
 * (SalesDashboardService). Chart data is pre-flattened into parallel
 * label/value lists rather than row DTOs - keeps the Chart.js inline
 * script in dashboard.html a plain list read, not a Spring EL
 * projection expression inside a JS-inlining comment.
 */
public class SalesDashboardStats {

    private final long todaySalesCount;
    private final BigDecimal todaySalesTotal;
    private final BigDecimal averageOrderValue;
    private final List<TopProductRow> topSellers;

    private final List<String> categoryLabels;
    private final List<BigDecimal> categoryRevenues;

    private final List<String> trendLabels;
    private final List<BigDecimal> trendRevenues;

    public SalesDashboardStats(long todaySalesCount, BigDecimal todaySalesTotal,
                                BigDecimal averageOrderValue, List<TopProductRow> topSellers,
                                List<String> categoryLabels, List<BigDecimal> categoryRevenues,
                                List<String> trendLabels, List<BigDecimal> trendRevenues) {
        this.todaySalesCount = todaySalesCount;
        this.todaySalesTotal = todaySalesTotal;
        this.averageOrderValue = averageOrderValue;
        this.topSellers = topSellers;
        this.categoryLabels = categoryLabels;
        this.categoryRevenues = categoryRevenues;
        this.trendLabels = trendLabels;
        this.trendRevenues = trendRevenues;
    }

    public long getTodaySalesCount() {
        return todaySalesCount;
    }

    public BigDecimal getTodaySalesTotal() {
        return todaySalesTotal;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public List<TopProductRow> getTopSellers() {
        return topSellers;
    }

    public List<String> getCategoryLabels() {
        return categoryLabels;
    }

    public List<BigDecimal> getCategoryRevenues() {
        return categoryRevenues;
    }

    public List<String> getTrendLabels() {
        return trendLabels;
    }

    public List<BigDecimal> getTrendRevenues() {
        return trendRevenues;
    }
}
