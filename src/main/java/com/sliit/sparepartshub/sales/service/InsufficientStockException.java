package com.sliit.sparepartshub.sales.service;

/**
 * UC-02 step 5a: cart can never claim more of a product than is
 * currently on the shelf, counting what's already in the cart.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName, int available, int alreadyInCart) {
        super(buildMessage(productName, available, alreadyInCart));
    }

    private static String buildMessage(String productName, int available, int alreadyInCart) {
        String base = "Only " + available + " unit(s) of \"" + productName + "\" in stock.";
        return alreadyInCart > 0
                ? base + " (" + alreadyInCart + " already in the cart.)"
                : base;
    }
}
