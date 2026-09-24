package com.craftbid.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateShipmentDTO {

    @NotBlank(message = "Courier/Carrier name is required")
    private String courierName;

    @NotBlank(message = "Tracking number (AWB) is required")
    private String trackingNumber;

    private BigDecimal shippingCost = BigDecimal.ZERO;

    private LocalDateTime pickupDate;

    private LocalDateTime estimatedDeliveryDate;

    private String trackingNotes;

    public CreateShipmentDTO() {
    }

    public CreateShipmentDTO(String courierName, String trackingNumber, BigDecimal shippingCost,
                             LocalDateTime pickupDate, LocalDateTime estimatedDeliveryDate, String trackingNotes) {
        this.courierName = courierName;
        this.trackingNumber = trackingNumber;
        this.shippingCost = shippingCost;
        this.pickupDate = pickupDate;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.trackingNotes = trackingNotes;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public LocalDateTime getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDateTime pickupDate) {
        this.pickupDate = pickupDate;
    }

    public LocalDateTime getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public String getTrackingNotes() {
        return trackingNotes;
    }

    public void setTrackingNotes(String trackingNotes) {
        this.trackingNotes = trackingNotes;
    }
}
