package com.sliit.sparepartshub.sales.dto;

import java.math.BigDecimal;

/**
 * View model for the Sales History list - date is pre-formatted here
 * rather than in the template, same reasoning as ReceiptLineItem doing
 * the subtotal math in Java instead of Thymeleaf/SpEL: keeps formatting
 * logic in one testable place instead of scattered across templates.
 */
public class SaleHistoryRow {

    private final Integer saleId;
    private final String saleCode;
    private final String formattedDate;
    private final String soldByName;
    private final BigDecimal amount;

    public SaleHistoryRow(Integer saleId, String saleCode, String formattedDate,
                           String soldByName, BigDecimal amount) {
        this.saleId = saleId;
        this.saleCode = saleCode;
        this.formattedDate = formattedDate;
        this.soldByName = soldByName;
        this.amount = amount;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public String getSaleCode() {
        return saleCode;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public String getSoldByName() {
        return soldByName;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
