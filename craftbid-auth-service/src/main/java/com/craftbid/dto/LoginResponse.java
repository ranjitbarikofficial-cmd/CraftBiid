package com.craftbid.dto;

public class LoginResponse {

    private String token;
    private Long userId;
    private String name;
    private String email;
    private String role;
    private boolean sellerEnabled;
    private String profileImageUrl;

    public LoginResponse(
            String token,
            Long userId,
            String name,
            String email,
            String role,
            boolean sellerEnabled) {
        this(token, userId, name, email, role, sellerEnabled, null);
    }

    public LoginResponse(
            String token,
            Long userId,
            String name,
            String email,
            String role,
            boolean sellerEnabled,
            String profileImageUrl) {

        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.sellerEnabled = sellerEnabled;
        this.profileImageUrl = profileImageUrl;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isSellerEnabled() {
        return sellerEnabled;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}