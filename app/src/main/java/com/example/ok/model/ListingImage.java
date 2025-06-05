package com.example.ok.model;

public class ListingImage {
    private Long id;
    private Long listingId;
    private String imageUrl;
    private Integer displayOrder;
    private String createdAt;

    // Constructors
    public ListingImage() {}

    public ListingImage(Long id, Long listingId, String imageUrl) {
        this.id = id;
        this.listingId = listingId;
        this.imageUrl = imageUrl;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getListingId() {
        return listingId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}