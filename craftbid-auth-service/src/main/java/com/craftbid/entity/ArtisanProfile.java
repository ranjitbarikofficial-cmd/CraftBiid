package com.craftbid.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "artisan_profiles")
public class ArtisanProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==========================================
    // USER
    // ==========================================

    @OneToOne
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    // ==========================================
    // ARTISAN INFORMATION
    // ==========================================

    @Column(name = "shop_name", nullable = false)
    private String shopName;

    @Column(name = "craft_type", nullable = false)
    private String craftType;

    @Column(nullable = false)
    private String city;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    // ==========================================
    // PAYOUT & BENEFICIARY DETAILS
    // ==========================================

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code", length = 20)
    private String bankIfscCode;

    @Column(name = "bank_account_name", length = 100)
    private String bankAccountName;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "payout_preference", length = 50)
    private String payoutPreference = "BANK_TRANSFER";

    // ==========================================
    // CREATED DATE
    // ==========================================

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public ArtisanProfile() {
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getProfileImageUrl() {
        return profileImageUrl != null ? profileImageUrl : (user != null ? user.getProfileImageUrl() : null);
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