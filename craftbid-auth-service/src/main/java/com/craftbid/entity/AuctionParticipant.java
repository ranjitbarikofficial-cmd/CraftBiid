package com.craftbid.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_participants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_auction_user_participant", columnNames = {"auction_id", "user_id"})
})
public class AuctionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePricePaid;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmountPaid;

    @Column(nullable = false)
    private String status = "JOINED"; // JOINED, ACTIVE, WON, REFUNDED

    @Column(precision = 12, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "cancellation_requested_at")
    private LocalDateTime cancellationRequestedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_fee", precision = 12, scale = 2)
    private BigDecimal cancellationFee = BigDecimal.ZERO;

    @Column(name = "cancellation_refund_amount", precision = 12, scale = 2)
    private BigDecimal cancellationRefundAmount = BigDecimal.ZERO;

    @Column(name = "cancellation_status", length = 50)
    private String cancellationStatus;

    public AuctionParticipant() {
    }

    public AuctionParticipant(Auction auction, User user, BigDecimal basePricePaid) {
        this.auction = auction;
        this.user = user;
        this.basePricePaid = basePricePaid;
        this.totalAmountPaid = basePricePaid;
        this.status = "JOINED";
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Auction getAuction() {
        return auction;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getBasePricePaid() {
        return basePricePaid;
    }

    public void setBasePricePaid(BigDecimal basePricePaid) {
        this.basePricePaid = basePricePaid;
    }

    public BigDecimal getTotalAmountPaid() {
        return totalAmountPaid;
    }

    public void setTotalAmountPaid(BigDecimal totalAmountPaid) {
        this.totalAmountPaid = totalAmountPaid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getCancellationRequestedAt() {
        return cancellationRequestedAt;
    }

    public void setCancellationRequestedAt(LocalDateTime cancellationRequestedAt) {
        this.cancellationRequestedAt = cancellationRequestedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public BigDecimal getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(BigDecimal cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    public BigDecimal getCancellationRefundAmount() {
        return cancellationRefundAmount;
    }

    public void setCancellationRefundAmount(BigDecimal cancellationRefundAmount) {
        this.cancellationRefundAmount = cancellationRefundAmount;
    }

    public String getCancellationStatus() {
        return cancellationStatus;
    }

    public void setCancellationStatus(String cancellationStatus) {
        this.cancellationStatus = cancellationStatus;
    }
}
