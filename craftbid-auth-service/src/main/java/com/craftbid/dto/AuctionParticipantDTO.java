package com.craftbid.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionParticipantDTO {

    private Long id;
    private Long userId;
    private String name;
    private String city;
    private BigDecimal basePricePaid;
    private BigDecimal totalAmountPaid;
    private String status;
    private LocalDateTime joinedAt;

    public AuctionParticipantDTO() {
    }

    public AuctionParticipantDTO(Long id, Long userId, String name, String city, BigDecimal basePricePaid, BigDecimal totalAmountPaid, String status, LocalDateTime joinedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.city = city;
        this.basePricePaid = basePricePaid;
        this.totalAmountPaid = totalAmountPaid;
        this.status = status;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
