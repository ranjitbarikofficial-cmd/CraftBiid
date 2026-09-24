package com.craftbid.dto;

import com.craftbid.entity.SellerSettlement;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SellerSettlementDTO {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long artisanId;
    private String artisanName;
    private BigDecimal winningAmount;
    private BigDecimal platformCommissionRate;
    private BigDecimal platformFee;
    private BigDecimal artisanPayout;
    private String status;
    private String payoutReference;
    private String payoutMethod;
    private String artisanBankDetails;
    private String artisanUpiId;
    private LocalDateTime settledAt;
    private String notes;
    private LocalDateTime createdAt;

    public SellerSettlementDTO() {
    }

    public static SellerSettlementDTO fromEntity(SellerSettlement settlement) {
        if (settlement == null) return null;
        SellerSettlementDTO dto = new SellerSettlementDTO();
        dto.setId(settlement.getId());
        if (settlement.getOrder() != null) {
            dto.setOrderId(settlement.getOrder().getId());
            dto.setOrderNumber(settlement.getOrder().getOrderNumber());
        }
        if (settlement.getArtisan() != null) {
            dto.setArtisanId(settlement.getArtisan().getId());
            dto.setArtisanName(settlement.getArtisan().getName());
        }
        dto.setWinningAmount(settlement.getWinningAmount());
        dto.setPlatformCommissionRate(settlement.getPlatformCommissionRate());
        dto.setPlatformFee(settlement.getPlatformFee());
        dto.setArtisanPayout(settlement.getArtisanPayout());
        dto.setStatus(settlement.getStatus());
        dto.setPayoutReference(settlement.getPayoutReference());
        dto.setPayoutMethod(settlement.getPayoutMethod());
        dto.setArtisanBankDetails(settlement.getArtisanBankDetails());
        dto.setArtisanUpiId(settlement.getArtisanUpiId());
        dto.setSettledAt(settlement.getSettledAt());
        dto.setNotes(settlement.getNotes());
        dto.setCreatedAt(settlement.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getArtisanId() {
        return artisanId;
    }

    public void setArtisanId(Long artisanId) {
        this.artisanId = artisanId;
    }

    public String getArtisanName() {
        return artisanName;
    }

    public void setArtisanName(String artisanName) {
        this.artisanName = artisanName;
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
}
