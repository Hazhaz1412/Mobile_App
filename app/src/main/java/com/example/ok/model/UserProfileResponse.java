package com.example.ok.model;

import java.math.BigDecimal;

public class UserProfileResponse {
    private Long userId;
    private String displayName;
    private String email;
    private String profilePictureUrl;
    private String bio;
    private String contactInfo;
    private BigDecimal ratingAvg;
    private Integer ratingCount;

    // Getters
    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public String getBio() {
        return bio;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    // Setters
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public void setRatingAvg(BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }
}
