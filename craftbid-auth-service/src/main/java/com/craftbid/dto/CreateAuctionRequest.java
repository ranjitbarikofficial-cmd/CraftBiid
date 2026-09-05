package com.craftbid.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateAuctionRequest {

    @NotNull(message = "Craft ID is required")
    @Positive(message = "Craft ID must be a positive number")
    private Long craftId;

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "1.00", message = "Starting price must be at least 1.00")
    @Digits(integer = 10, fraction = 2, message = "Starting price format is invalid (max 10 digits and 2 decimals)")
    private BigDecimal startingPrice;

    @DecimalMin(value = "0.00", message = "Reserve price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Reserve price format is invalid")
    private BigDecimal reservePrice;

    @DecimalMin(value = "1.00", message = "Minimum bid increment must be at least 1.00")
    @Digits(integer = 8, fraction = 2, message = "Minimum bid increment format is invalid")
    private BigDecimal minBidIncrement;

    @Min(value = 1, message = "Duration must be at least 1 hour")
    @Max(value = 720, message = "Duration cannot exceed 720 hours (30 days)")
    private Integer durationHours;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public CreateAuctionRequest() {
    }

    public Long getCraftId() {
        return craftId;
    }

    public void setCraftId(Long craftId) {
        this.craftId = craftId;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice;
    }

    public BigDecimal getMinBidIncrement() {
        return minBidIncrement;
    }

    public void setMinBidIncrement(BigDecimal minBidIncrement) {
        this.minBidIncrement = minBidIncrement;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
