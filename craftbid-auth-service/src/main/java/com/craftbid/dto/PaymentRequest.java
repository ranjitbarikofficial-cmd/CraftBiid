package com.craftbid.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PaymentRequest {

    @Positive(message = "Auction ID must be a positive number")
    private Long auctionId;

    @Positive(message = "Craft ID must be a positive number")
    private Long craftId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "1.00", message = "Payment amount must be at least 1.00")
    @Digits(integer = 10, fraction = 2, message = "Payment amount format is invalid")
    private BigDecimal amount;

    @NotBlank(message = "Payment type is required")
    @Pattern(regexp = "^(BASE_DEPOSIT|DIFFERENTIAL_BID|DIRECT_PURCHASE|ORDER_SETTLEMENT)$", message = "Invalid payment type")
    private String type; // BASE_DEPOSIT, DIFFERENTIAL_BID, DIRECT_PURCHASE

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(UPI|CARD|NETBANKING|WALLET|COD)$", message = "Invalid payment method (UPI, CARD, NETBANKING, WALLET, COD)")
    private String paymentMethod; // UPI, CARD, NETBANKING, WALLET

    @Size(max = 255, message = "Notes cannot exceed 255 characters")
    private String notes;

    public PaymentRequest() {
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
