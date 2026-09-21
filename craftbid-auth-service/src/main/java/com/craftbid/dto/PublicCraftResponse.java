package com.craftbid.dto;

import com.craftbid.entity.Craft;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PublicCraftResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal basePrice;
    private String imageUrl;
    private String status;
    private PublicSellerResponse seller;
    private PublicCategoryResponse category;
    private LocalDateTime createdAt;

    public PublicCraftResponse() {
    }

    public static PublicCraftResponse fromEntity(Craft craft) {
        if (craft == null) {
            return null;
        }
        PublicCraftResponse res = new PublicCraftResponse();
        res.setId(craft.getId());
        res.setTitle(craft.getTitle());
        res.setDescription(craft.getDescription());
        res.setBasePrice(craft.getBasePrice());
        res.setImageUrl(craft.getImageUrl());
        res.setStatus(craft.getStatus());
        res.setSeller(PublicSellerResponse.fromUser(craft.getSeller()));
        res.setCategory(PublicCategoryResponse.fromEntity(craft.getCategory()));
        res.setCreatedAt(craft.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PublicSellerResponse getSeller() {
        return seller;
    }

    public void setSeller(PublicSellerResponse seller) {
        this.seller = seller;
    }

    public PublicCategoryResponse getCategory() {
        return category;
    }

    public void setCategory(PublicCategoryResponse category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
