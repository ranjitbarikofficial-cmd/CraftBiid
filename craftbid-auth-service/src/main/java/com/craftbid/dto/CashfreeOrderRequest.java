package com.craftbid.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CashfreeOrderRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least ₹1.00")
    private BigDecimal amount;

    private Long auctionId;

    private Long craftId;

    private String type = "PARTICIPATION"; // PARTICIPATION, DIFFERENTIAL_BID, DIRECT_PURCHASE

    public CashfreeOrderRequest() {
    }

    public CashfreeOrderRequest(BigDecimal amount, Long auctionId, Long craftId, String type) {
        this.amount = amount;
        this.auctionId = auctionId;
        this.craftId = craftId;
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getCraftId() {
        return craftId;
    }

    public void setCraftId(Long craftId) {
        this.craftId = craftId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
