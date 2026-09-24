package com.craftbid.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class PackageDetailsDTO {

    @NotNull(message = "Package weight is required")
    @DecimalMin(value = "0.01", message = "Weight must be greater than 0 kg")
    private Double weight; // in kg

    @NotNull(message = "Package length is required")
    @DecimalMin(value = "0.1", message = "Length must be greater than 0 cm")
    private Double length; // in cm

    @NotNull(message = "Package width is required")
    @DecimalMin(value = "0.1", message = "Width must be greater than 0 cm")
    private Double width; // in cm

    @NotNull(message = "Package height is required")
    @DecimalMin(value = "0.1", message = "Height must be greater than 0 cm")
    private Double height; // in cm

    private String notes;

    public PackageDetailsDTO() {
    }

    public PackageDetailsDTO(Double weight, Double length, Double width, Double height, String notes) {
        this.weight = weight;
        this.length = length;
        this.width = width;
        this.height = height;
        this.notes = notes;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
