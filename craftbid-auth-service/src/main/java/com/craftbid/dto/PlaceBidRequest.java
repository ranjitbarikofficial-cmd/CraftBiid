package com.craftbid.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PlaceBidRequest {

    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "1.00", message = "Bid amount must be at least 1.00")
    @Digits(integer = 10, fraction = 2, message = "Bid amount format is invalid")
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
