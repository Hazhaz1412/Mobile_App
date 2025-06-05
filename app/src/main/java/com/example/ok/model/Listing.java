package com.example.ok.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class Listing {
    private Long id;
    private Long userId;
    private String userDisplayName;
    private String title;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private String categoryName;
    private Long conditionId;
    private String conditionName;
    private String locationText;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String status;
    private Integer viewCount;
    private Integer interactionCount;
    private String createdAt;
    private String updatedAt;
    private List<String> imageUrls;
    private String primaryImageUrl;
    private List<String> tags;

    // Constructors
    public Listing() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserDisplayName() { return userDisplayName; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Long getConditionId() { return conditionId; }
    public void setConditionId(Long conditionId) { this.conditionId = conditionId; }

    public String getConditionName() { return conditionName; }
    public void setConditionName(String conditionName) { this.conditionName = conditionName; }

    public String getLocationText() { return locationText; }
    public void setLocationText(String locationText) { this.locationText = locationText; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getInteractionCount() { return interactionCount; }
    public void setInteractionCount(Integer interactionCount) { this.interactionCount = interactionCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getPrimaryImageUrl() { return primaryImageUrl; }
    public void setPrimaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Integer getViews() {
        return viewCount != null ? viewCount : 0;
    }



    // Thêm getters for category và condition objects (nếu cần)
    public Category getCategory() {
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);
        return category;
    }

    public ItemCondition getCondition() {
        ItemCondition condition = new ItemCondition();
        condition.setId(conditionId);
        condition.setName(conditionName);
        return condition;
    }

    // Thêm getter for images (nếu cần)
    public List<ListingImage> getImages() {
        List<ListingImage> images = new ArrayList<>();
        if (imageUrls != null) {
            for (int i = 0; i < imageUrls.size(); i++) {
                ListingImage image = new ListingImage();
                image.setId((long) i);
                image.setImageUrl(imageUrls.get(i));
                image.setListingId(this.id);
                images.add(image);
            }
        }
        return images;
    }

    public Date getCreatedAtAsDate() {
        try {
            if (createdAt != null) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                return format.parse(createdAt);
            }
        } catch (Exception e) {
            // Return current date if parsing fails
        }
        return new Date();
    }
}