package com.craftbid.dto;

import com.craftbid.entity.User;

public class PublicSellerResponse {

    private Long id;
    private String name;
    private String displayName;
    private String city;
    private String profileImageUrl;
    private boolean verified;
    private boolean sellerEnabled;

    public PublicSellerResponse() {
    }

    public PublicSellerResponse(
            Long id,
            String name,
            String displayName,
            String city,
            String profileImageUrl,
            boolean verified,
            boolean sellerEnabled) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.city = city;
        this.profileImageUrl = profileImageUrl;
        this.verified = verified;
        this.sellerEnabled = sellerEnabled;
    }

    public static PublicSellerResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        String displayName = user.getName() != null && !user.getName().isBlank()
                ? user.getName()
                : "Master Artisan";
        String city = user.getCity() != null && !user.getCity().isBlank()
                ? user.getCity()
                : "India";
        boolean verified = user.isSellerEnabled();

        return new PublicSellerResponse(
                user.getId(),
                user.getName(),
                displayName,
                city,
                user.getProfileImageUrl(),
                verified,
                user.isSellerEnabled()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isSellerEnabled() {
        return sellerEnabled;
    }

    public void setSellerEnabled(boolean sellerEnabled) {
        this.sellerEnabled = sellerEnabled;
    }
}
