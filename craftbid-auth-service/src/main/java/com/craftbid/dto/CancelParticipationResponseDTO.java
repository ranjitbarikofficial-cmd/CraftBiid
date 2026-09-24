package com.craftbid.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CancelParticipationResponseDTO {

    private Long auctionId;
    private Long participantId;
    private BigDecimal totalAmountPaid;
    private BigDecimal cancellationFeePercent;
    private BigDecimal cancellationFee;
    private BigDecimal refundAmount;
    private String refundTransactionRef;
    private String status; // "CANCELLED"
    private String message;
    private int currentParticipantsCount;
    private LocalDateTime cancelledAt;

    public CancelParticipationResponseDTO() {
    }

    public CancelParticipationResponseDTO(
            Long auctionId,
            Long participantId,
            BigDecimal totalAmountPaid,
            BigDecimal cancellationFeePercent,
            BigDecimal cancellationFee,
            BigDecimal refundAmount,
            String refundTransactionRef,
            String status,
            String message,
            int currentParticipantsCount,
            LocalDateTime cancelledAt) {
        this.auctionId = auctionId;
        this.participantId = participantId;
        this.totalAmountPaid = totalAmountPaid;
        this.cancellationFeePercent = cancellationFeePercent;
        this.cancellationFee = cancellationFee;
        this.refundAmount = refundAmount;
        this.refundTransactionRef = refundTransactionRef;
        this.status = status;
        this.message = message;
        this.currentParticipantsCount = currentParticipantsCount;
        this.cancelledAt = cancelledAt;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }

    public BigDecimal getTotalAmountPaid() {
        return totalAmountPaid;
    }

    public void setTotalAmountPaid(BigDecimal totalAmountPaid) {
        this.totalAmountPaid = totalAmountPaid;
    }

    public BigDecimal getCancellationFeePercent() {
        return cancellationFeePercent;
    }

    public void setCancellationFeePercent(BigDecimal cancellationFeePercent) {
        this.cancellationFeePercent = cancellationFeePercent;
    }

    public BigDecimal getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(BigDecimal cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundTransactionRef() {
        return refundTransactionRef;
    }

    public void setRefundTransactionRef(String refundTransactionRef) {
        this.refundTransactionRef = refundTransactionRef;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCurrentParticipantsCount() {
        return currentParticipantsCount;
    }

    public void setCurrentParticipantsCount(int currentParticipantsCount) {
        this.currentParticipantsCount = currentParticipantsCount;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
