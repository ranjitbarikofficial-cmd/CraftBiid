package com.craftbid.dto;

public class EmailVerificationRequest {

    private String email;
    private String phone;
    private String identifier;
    private String otp;

    public EmailVerificationRequest() {
    }

    public EmailVerificationRequest(String identifier, String otp) {
        this.identifier = identifier;
        this.otp = otp;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getEffectiveIdentifier() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        if (phone != null && !phone.isBlank()) {
            return phone.trim();
        }
        return null;
    }
}