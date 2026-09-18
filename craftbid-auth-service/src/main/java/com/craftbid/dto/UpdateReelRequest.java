package com.craftbid.dto;

import jakarta.validation.constraints.Size;

public class UpdateReelRequest {

    @Size(min = 2, max = 150, message = "Title must be between 2 and 150 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @Size(max = 500, message = "Thumbnail URL cannot exceed 500 characters")
    private String thumbnailUrl;

    @Size(max = 500, message = "Video URL cannot exceed 500 characters")
    private String videoUrl;

    public UpdateReelRequest() {
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

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
