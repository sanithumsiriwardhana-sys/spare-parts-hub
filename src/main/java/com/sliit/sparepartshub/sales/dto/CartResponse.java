package com.sliit.sparepartshub.sales.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the front-end gets back after any cart mutation (add/remove) -
 * the full current cart state plus any compatibility conflicts the last
 * add triggered, so the JS can re-render the cart panel and the warning
 * banner from one response instead of two round trips.
 */
public class CartResponse {

    private final List<CartItem> items;
    private final BigDecimal total;
    private final int itemCount;
    private final List<CompatibilityConflict> conflicts;

    public CartResponse(List<CartItem> items, BigDecimal total, int itemCount,
                         List<CompatibilityConflict> conflicts) {
        this.items = items;
        this.total = total;
        this.itemCount = itemCount;
        this.conflicts = conflicts;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public int getItemCount() {
        return itemCount;
    }

    public List<CompatibilityConflict> getConflicts() {
        return conflicts;
    }
}
