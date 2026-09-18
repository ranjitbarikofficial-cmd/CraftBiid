package com.craftbid.dto;

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
}
