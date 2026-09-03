package com.craftbid.dto;

import java.math.BigDecimal;

public class PlaceBidRequest {

    private BigDecimal amount;

    public PlaceBidRequest() {
    }

    public PlaceBidRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
