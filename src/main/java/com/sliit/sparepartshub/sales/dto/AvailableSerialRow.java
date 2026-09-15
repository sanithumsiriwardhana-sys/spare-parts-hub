package com.sliit.sparepartshub.sales.dto;

/**
 * One in-stock serial number available to select when adding a
 * serial-tracked product to the cart. receivedDate is pre-formatted -
 * same reasoning as ReceiptLineItem/SaleHistoryRow, keep date formatting
 * in Java, not the template.
 */
public class AvailableSerialRow {

    private final Integer serialId;
    private final String serialValue;
    private final String receivedDateLabel;

    public AvailableSerialRow(Integer serialId, String serialValue, String receivedDateLabel) {
        this.serialId = serialId;
        this.serialValue = serialValue;
        this.receivedDateLabel = receivedDateLabel;
    }

    public Integer getSerialId() {
        return serialId;
    }

    public String getSerialValue() {
        return serialValue;
    }

    public String getReceivedDateLabel() {
        return receivedDateLabel;
    }
}
