package com.craftbid.dto;

import com.craftbid.entity.ArtisanProfile;
import com.craftbid.entity.User;

import java.time.LocalDateTime;

public class ArtisanProfileDTO {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String city;
    private String shopName;
    private String craftType;
    private String profileImageUrl;
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankAccountName;
    private String upiId;
    private String payoutPreference;
    private LocalDateTime createdAt;

    public ArtisanProfileDTO() {
    }

    public ArtisanProfileDTO(
            Long id,
            Long userId,
            String name,
            String email,
            String phone,
            String city,
            String shopName,
            String craftType,
            String profileImageUrl,
            LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.city = city;
        this.shopName = shopName;
        this.craftType = craftType;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
    }

    public static ArtisanProfileDTO fromEntity(ArtisanProfile profile, User user) {
        if (profile == null && user == null) return null;
        ArtisanProfileDTO dto = new ArtisanProfileDTO();
        if (profile != null) {
            dto.setId(profile.getId());
            dto.setShopName(profile.getShopName());
            dto.setCraftType(profile.getCraftType());
            dto.setCity(profile.getCity());
            dto.setProfileImageUrl(profile.getProfileImageUrl());
            dto.setBankAccountNumber(profile.getBankAccountNumber());
            dto.setBankIfscCode(profile.getBankIfscCode());
            dto.setBankAccountName(profile.getBankAccountName());
            dto.setUpiId(profile.getUpiId());
            dto.setPayoutPreference(profile.getPayoutPreference());
            dto.setCreatedAt(profile.getCreatedAt());
        }
        if (user != null) {
            dto.setUserId(user.getId());
            dto.setName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setPhone(user.getPhone());
            if (dto.getCity() == null) dto.setCity(user.getCity());
            if (dto.getProfileImageUrl() == null) dto.setProfileImageUrl(user.getProfileImageUrl());
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
