package com.craftbid.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateArtisanProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(min = 2, max = 100, message = "Shop name must be between 2 and 100 characters")
    private String shopName;

    @Size(min = 2, max = 100, message = "Craft type must be between 2 and 100 characters")
    private String craftType;

    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be a valid 10-digit number")
    private String phone;

    public UpdateArtisanProfileRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
