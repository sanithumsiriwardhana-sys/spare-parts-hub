package com.sliit.sparepartshub.sales.service;

import com.sliit.sparepartshub.sales.dto.PriceChangeRow;

import java.util.List;

/**
 * UC-02 extension 10a - thrown during checkout when one or more cart
 * items have a price in the DB that no longer matches what they were
 * added to the cart at, and the caller hasn't yet confirmed proceeding
 * at the new price(s). Mirrors CompatibilityOverrideRequiredException's
 * shape: the controller catches this, returns 409 with the deltas, and
 * the client re-submits with confirmPriceChanges=true once the cashier
 * has seen and accepted the new totals.
 */
public class PriceChangeConfirmationRequiredException extends RuntimeException {

    private final List<PriceChangeRow> priceChanges;

    public PriceChangeConfirmationRequiredException(List<PriceChangeRow> priceChanges) {
        super("Price changes detected since items were added to cart.");
        this.priceChanges = priceChanges;
    }

    public List<PriceChangeRow> getPriceChanges() {
        return priceChanges;
    }
}
