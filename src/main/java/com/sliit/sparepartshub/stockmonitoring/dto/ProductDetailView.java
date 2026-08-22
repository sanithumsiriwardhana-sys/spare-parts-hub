package com.sliit.sparepartshub.stockmonitoring.dto;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.SaleItem;
import com.sliit.sparepartshub.entity.StockRequest;

import java.util.List;

/**
 * Everything UC-03 step 8 asks for when a Supervisor drills into one
 * product: "recent sales, estimated stock coverage, open customer
 * requests, incoming quantities, and a suggested restock quantity."
 */
public class ProductDetailView {

    private final Product product;
    private final UrgencyLevel level;
    private final List<SaleItem> recentSales;
    private final long unitsSoldInWindow;
    private final Double stockCoverageDays; // null = no recent sales, can't estimate
    private final List<StockRequest> openStockRequests;
    private final int incomingQuantity;
    private final int suggestedRestockQuantity;
    private final boolean suggestionIsLiveEstimate; // true if no persisted suggestion exists yet

    public ProductDetailView(Product product, UrgencyLevel level, List<SaleItem> recentSales,
                              long unitsSoldInWindow, Double stockCoverageDays,
                              List<StockRequest> openStockRequests, int incomingQuantity,
                              int suggestedRestockQuantity, boolean suggestionIsLiveEstimate) {
        this.product = product;
        this.level = level;
        this.recentSales = recentSales;
        this.unitsSoldInWindow = unitsSoldInWindow;
        this.stockCoverageDays = stockCoverageDays;
        this.openStockRequests = openStockRequests;
        this.incomingQuantity = incomingQuantity;
        this.suggestedRestockQuantity = suggestedRestockQuantity;
        this.suggestionIsLiveEstimate = suggestionIsLiveEstimate;
    }

    public Product getProduct() {
        return product;
    }

    public UrgencyLevel getLevel() {
        return level;
    }

    public List<SaleItem> getRecentSales() {
        return recentSales;
    }

    public long getUnitsSoldInWindow() {
        return unitsSoldInWindow;
    }

    public Double getStockCoverageDays() {
        return stockCoverageDays;
    }

    public List<StockRequest> getOpenStockRequests() {
        return openStockRequests;
    }

    public int getIncomingQuantity() {
        return incomingQuantity;
    }

    public int getSuggestedRestockQuantity() {
        return suggestedRestockQuantity;
    }

    public boolean isSuggestionIsLiveEstimate() {
        return suggestionIsLiveEstimate;
    }
}
