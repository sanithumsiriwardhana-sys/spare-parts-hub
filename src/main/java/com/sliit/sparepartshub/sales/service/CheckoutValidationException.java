package com.sliit.sparepartshub.sales.service;

/**
 * UC-02 step 10a: something about the cart is no longer valid by the
 * time Complete Checkout is pressed - a product went out of stock, was
 * removed, etc. Distinct from InsufficientStockException (raised on Add
 * to Cart) because the message here needs to guide the Sales Executive
 * to fix their cart, not just report why one add failed.
 */
public class CheckoutValidationException extends RuntimeException {

    public CheckoutValidationException(String message) {
        super(message);
    }
}
