package com.craftbid.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class CashfreeVerifyRequest {

    @NotBlank(message = "Cashfree order ID is required")
    private String orderId;

    private Long auctionId;

    private Long craftId;

    private BigDecimal amount;

    private String type = "PARTICIPATION";

    private String paymentMethod = "CASHFREE";

    public CashfreeVerifyRequest() {
    }

    public CashfreeVerifyRequest(String orderId, Long auctionId, Long craftId, BigDecimal amount, String type, String paymentMethod) {
        this.orderId = orderId;
        this.auctionId = auctionId;
        this.craftId = craftId;
        this.amount = amount;
        this.type = type;
        this.paymentMethod = paymentMethod;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
