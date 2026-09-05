package com.craftbid.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmailVerificationRequest {

    @Email(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email format")
    @Size(max = 120, message = "Email cannot exceed 120 characters")
    private String email;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be a valid 10 to 15 digit number")
    @Size(min = 10, max = 15, message = "Phone must be between 10 and 15 digits")
    private String phone;

    @Size(min = 3, max = 120, message = "Identifier must be between 3 and 120 characters")
    private String identifier;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 numeric digits")
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