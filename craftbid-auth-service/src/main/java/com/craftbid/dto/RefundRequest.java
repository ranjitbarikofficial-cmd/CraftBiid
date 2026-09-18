package com.craftbid.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class RefundRequest {

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "1.0", message = "Refund amount must be at least ₹1.00")
    private BigDecimal amount;

    private String reason;

    public RefundRequest() {
    }

    public RefundRequest(BigDecimal amount, String reason) {
        this.amount = amount;
        this.reason = reason;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
