package com.craftbid.dto;

import jakarta.validation.constraints.NotBlank;

public class ArtisanRegistrationRequest {

    @NotBlank(message = "Shop name is required")
    private String shopName;

    @NotBlank(message = "Craft type is required")
    private String craftType;

    @NotBlank(message = "City is required")
    private String city;

    public ArtisanRegistrationRequest() {
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getCraftType() {
        return craftType;
    }

    public void setCraftType(String craftType) {
        this.craftType = craftType;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}