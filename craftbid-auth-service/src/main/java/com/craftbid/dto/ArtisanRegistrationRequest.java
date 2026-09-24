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

    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankAccountName;
    private String upiId;
    private String payoutPreference;

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

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankIfscCode() {
        return bankIfscCode;
    }

    public void setBankIfscCode(String bankIfscCode) {
        this.bankIfscCode = bankIfscCode;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getPayoutPreference() {
        return payoutPreference;
    }

    public void setPayoutPreference(String payoutPreference) {
        this.payoutPreference = payoutPreference;
    }
}