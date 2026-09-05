package com.craftbid.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class JoinAuctionRequest {

    @Pattern(regexp = "^(UPI|CARD|NETBANKING|WALLET|COD)$", message = "Payment method must be UPI, CARD, NETBANKING, WALLET, or COD")
    private String paymentMethod = "UPI";

    @Size(max = 100, message = "Transaction reference cannot exceed 100 characters")
    private String transactionRef;

    public JoinAuctionRequest() {
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }
}
