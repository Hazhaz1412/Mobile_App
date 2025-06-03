package com.example.ok.model;

public class UserProfileRequest {
    private String displayName;
    private String bio;
    private String contactInfo;

    public UserProfileRequest(String displayName, String bio, String contactInfo) {
        this.displayName = displayName;
        this.bio = bio;
        this.contactInfo = contactInfo;
    }

    // Getters
    public String getDisplayName() {
        return displayName;
    }

    public String getBio() {
        return bio;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    // Setters
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
}
