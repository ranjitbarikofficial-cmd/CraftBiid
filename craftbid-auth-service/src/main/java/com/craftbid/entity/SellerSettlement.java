package com.craftbid.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_settlements")
public class SellerSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @JsonIgnore
    private AuctionOrder order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artisan_id", nullable = false)
    private User artisan;

    @Column(name = "winning_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal winningAmount;

    @Column(name = "platform_commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal platformCommissionRate = new BigDecimal("10.00"); // 10%

    @Column(name = "platform_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal platformFee; // 10% of winningAmount

    @Column(name = "artisan_payout", nullable = false, precision = 12, scale = 2)
    private BigDecimal artisanPayout; // 90% of winningAmount

    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, PROCESSING, PAID, CANCELLED

    @Column(name = "payout_reference", length = 100)
    private String payoutReference;

    @Column(name = "payout_method", length = 50)
    private String payoutMethod = "BANK_TRANSFER";

    @Column(name = "artisan_bank_details", length = 255)
    private String artisanBankDetails;

    @Column(name = "artisan_upi_id", length = 100)
    private String artisanUpiId;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public SellerSettlement() {
    }

    public SellerSettlement(AuctionOrder order, User artisan, BigDecimal winningAmount, BigDecimal platformFee, BigDecimal artisanPayout) {
        this.order = order;
        this.artisan = artisan;
        this.winningAmount = winningAmount;
        this.platformCommissionRate = new BigDecimal("10.00");
        this.platformFee = platformFee;
        this.artisanPayout = artisanPayout;
        this.status = "PENDING";
        this.payoutMethod = "BANK_TRANSFER";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuctionOrder getOrder() {
        return order;
    }

    public void setOrder(AuctionOrder order) {
        this.order = order;
    }

    public User getArtisan() {
        return artisan;
    }

    public void setArtisan(User artisan) {
        this.artisan = artisan;
    }

    public BigDecimal getWinningAmount() {
        return winningAmount;
    }

    public void setWinningAmount(BigDecimal winningAmount) {
        this.winningAmount = winningAmount;
    }

    public BigDecimal getPlatformCommissionRate() {
        return platformCommissionRate;
    }

    public void setPlatformCommissionRate(BigDecimal platformCommissionRate) {
        this.platformCommissionRate = platformCommissionRate;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getArtisanPayout() {
        return artisanPayout;
    }

    public void setArtisanPayout(BigDecimal artisanPayout) {
        this.artisanPayout = artisanPayout;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPayoutReference() {
        return payoutReference;
    }

    public void setPayoutReference(String payoutReference) {
        this.payoutReference = payoutReference;
    }

    public String getPayoutMethod() {
        return payoutMethod;
    }

    public void setPayoutMethod(String payoutMethod) {
        this.payoutMethod = payoutMethod;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getArtisanBankDetails() {
        return artisanBankDetails;
    }

    public void setArtisanBankDetails(String artisanBankDetails) {
        this.artisanBankDetails = artisanBankDetails;
    }

    public String getArtisanUpiId() {
        return artisanUpiId;
    }

    public void setArtisanUpiId(String artisanUpiId) {
        this.artisanUpiId = artisanUpiId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
