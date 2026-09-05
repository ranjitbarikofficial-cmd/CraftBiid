package com.craftbid.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ArtisanRegistrationRequest {

    @NotBlank(message = "Shop name is required")
    @Size(min = 2, max = 100, message = "Shop name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s.'&/()-]+$", message = "Shop name contains invalid characters")
    private String shopName;

    @NotBlank(message = "Craft type is required")
    @Size(min = 2, max = 100, message = "Craft type must be between 2 and 100 characters")
    private String craftType;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.'-]+$", message = "City must contain only letters, spaces, dots, hyphens")
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