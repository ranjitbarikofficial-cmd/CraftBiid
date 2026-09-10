package com.craftbid.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateOrderStatusRequest {

    @NotBlank(message = "Status is required")
    private String status; // PAID, PACKED, SHIPPED, DELIVERED

    private String trackingNotes;

    private String carrier;

    public UpdateOrderStatusRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTrackingNotes() {
        return trackingNotes;
    }

    public void setTrackingNotes(String trackingNotes) {
        this.trackingNotes = trackingNotes;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }
}
