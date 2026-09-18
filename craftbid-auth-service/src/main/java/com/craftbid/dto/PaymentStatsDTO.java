package com.craftbid.dto;

import java.math.BigDecimal;

public class PaymentStatsDTO {

    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private long totalRefunds;
    private BigDecimal totalVolume;
    private BigDecimal totalRefundedVolume;
    private BigDecimal netVolume;
    private String currency;

    public PaymentStatsDTO() {
    }

    public PaymentStatsDTO(long totalTransactions, long successfulTransactions, long failedTransactions, long totalRefunds, BigDecimal totalVolume, BigDecimal totalRefundedVolume, BigDecimal netVolume, String currency) {
        this.totalTransactions = totalTransactions;
        this.successfulTransactions = successfulTransactions;
        this.failedTransactions = failedTransactions;
        this.totalRefunds = totalRefunds;
        this.totalVolume = totalVolume;
        this.totalRefundedVolume = totalRefundedVolume;
        this.netVolume = netVolume;
        this.currency = currency;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getSuccessfulTransactions() {
        return successfulTransactions;
    }

    public void setSuccessfulTransactions(long successfulTransactions) {
        this.successfulTransactions = successfulTransactions;
    }

    public long getFailedTransactions() {
        return failedTransactions;
    }

    public void setFailedTransactions(long failedTransactions) {
        this.failedTransactions = failedTransactions;
    }

    public long getTotalRefunds() {
        return totalRefunds;
    }

    public void setTotalRefunds(long totalRefunds) {
        this.totalRefunds = totalRefunds;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(BigDecimal totalVolume) {
        this.totalVolume = totalVolume;
    }

    public BigDecimal getTotalRefundedVolume() {
        return totalRefundedVolume;
    }

    public void setTotalRefundedVolume(BigDecimal totalRefundedVolume) {
        this.totalRefundedVolume = totalRefundedVolume;
    }

    public BigDecimal getNetVolume() {
        return netVolume;
    }

    public void setNetVolume(BigDecimal netVolume) {
        this.netVolume = netVolume;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
