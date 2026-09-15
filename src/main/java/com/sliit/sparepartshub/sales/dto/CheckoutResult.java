package com.sliit.sparepartshub.sales.dto;

import java.math.BigDecimal;

public class CheckoutResult {

    private final Integer saleId;
    private final String saleCode;
    private final String ticketCode;
    private final BigDecimal total;
    private final int itemCount;

    public CheckoutResult(Integer saleId, String saleCode, String ticketCode,
                           BigDecimal total, int itemCount) {
        this.saleId = saleId;
        this.saleCode = saleCode;
        this.ticketCode = ticketCode;
        this.total = total;
        this.itemCount = itemCount;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public String getSaleCode() {
        return saleCode;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public int getItemCount() {
        return itemCount;
    }
}
