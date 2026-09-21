package com.craftbid.dto;

import com.craftbid.entity.Category;

public class PublicCategoryResponse {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;

    public PublicCategoryResponse() {
    }

    public PublicCategoryResponse(Long id, String name, String description, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public static PublicCategoryResponse fromEntity(Category category) {
        if (category == null) {
            return null;
        }
        return new PublicCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getImageUrl()
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
