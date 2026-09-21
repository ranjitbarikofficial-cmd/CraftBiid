package com.craftbid.dto;

import com.craftbid.entity.CraftReel;

import java.time.LocalDateTime;

public class PublicCraftReelResponse {

    private Long id;
    private PublicCraftResponse craft;
    private PublicSellerResponse artisan;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private Long views;
    private Long likes;
    private String status;
    private LocalDateTime createdAt;

    public PublicCraftReelResponse() {
    }

    public static PublicCraftReelResponse fromEntity(CraftReel reel) {
        if (reel == null) {
            return null;
        }
        PublicCraftReelResponse res = new PublicCraftReelResponse();
        res.setId(reel.getId());
        res.setCraft(PublicCraftResponse.fromEntity(reel.getCraft()));
        if (reel.getArtisan() != null && reel.getArtisan().getUser() != null) {
            res.setArtisan(PublicSellerResponse.fromUser(reel.getArtisan().getUser()));
        } else if (reel.getCraft() != null && reel.getCraft().getSeller() != null) {
            res.setArtisan(PublicSellerResponse.fromUser(reel.getCraft().getSeller()));
        }
        res.setTitle(reel.getTitle());
        res.setDescription(reel.getDescription());
        res.setVideoUrl(reel.getVideoUrl());
        res.setThumbnailUrl(reel.getThumbnailUrl());
        res.setViews(reel.getViews());
        res.setLikes(reel.getLikes());
        res.setStatus(reel.getStatus());
        res.setCreatedAt(reel.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PublicCraftResponse getCraft() {
        return craft;
    }

    public void setCraft(PublicCraftResponse craft) {
        this.craft = craft;
    }

    public PublicSellerResponse getArtisan() {
        return artisan;
    }

    public void setArtisan(PublicSellerResponse artisan) {
        this.artisan = artisan;
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

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Long getViews() {
        return views;
    }

    public void setViews(Long views) {
        this.views = views;
    }

    public Long getLikes() {
        return likes;
    }

    public void setLikes(Long likes) {
        this.likes = likes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
