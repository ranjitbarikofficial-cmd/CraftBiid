package com.craftbid.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SubmitAddressRequest {

    private Long addressId;

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be a valid 10 to 15 digit number")
    private String phone;

    @Size(min = 5, max = 255, message = "Address line 1 must be between 5 and 255 characters")
    private String addressLine1;

    private String addressLine2;

    private String streetAddress; // alias for backwards compatibility

    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;

    @Size(min = 2, max = 100, message = "State must be between 2 and 100 characters")
    private String state;

    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 numeric digits")
    private String pincode;

    private String landmark;

    private Boolean saveAsDefault = false;

    public SubmitAddressRequest() {
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine1() {
        if (addressLine1 != null && !addressLine1.isBlank()) {
            return addressLine1;
        }
        return streetAddress;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getStreetAddress() {
        return getAddressLine1();
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
        if (this.addressLine1 == null) {
            this.addressLine1 = streetAddress;
        }
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public Boolean getSaveAsDefault() {
        return saveAsDefault;
    }

    public void setSaveAsDefault(Boolean saveAsDefault) {
        this.saveAsDefault = saveAsDefault;
    }
}
