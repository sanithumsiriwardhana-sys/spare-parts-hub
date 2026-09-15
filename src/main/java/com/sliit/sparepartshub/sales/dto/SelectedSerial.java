package com.sliit.sparepartshub.sales.dto;

/**
 * One serial number the Sales Executive selected for a serial-tracked
 * cart item (e.g. a specific GPU unit). Lives inside CartItem's
 * selectedSerials list in the session cart.
 */
public class SelectedSerial {

    private final Integer serialId;
    private final String serialValue;

    public SelectedSerial(Integer serialId, String serialValue) {
        this.serialId = serialId;
        this.serialValue = serialValue;
    }

    public Integer getSerialId() {
        return serialId;
    }

    public String getSerialValue() {
        return serialValue;
    }
}
