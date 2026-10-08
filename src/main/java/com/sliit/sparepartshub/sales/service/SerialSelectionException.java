package com.sliit.sparepartshub.sales.service;

/**
 * UC-02 precondition 4 / step 13: a serial-tracked item needs exactly
 * one selected serial per unit, each one actually available and not
 * already claimed elsewhere in this cart.
 */
public class SerialSelectionException extends RuntimeException {

    public SerialSelectionException(String message) {
        super(message);
    }
}
