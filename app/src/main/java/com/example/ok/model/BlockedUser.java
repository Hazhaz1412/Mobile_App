package com.example.ok.model;

import java.util.Date;

public class BlockedUser {
    private Long userId;
    private String displayName;
    private String profilePicture;
    private Date blockedAt;

    // Constructors
    public BlockedUser() {}

    public BlockedUser(Long userId, String displayName, String profilePicture, Date blockedAt) {
        this.userId = userId;
        this.displayName = displayName;
        this.profilePicture = profilePicture;
        this.blockedAt = blockedAt;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public Date getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(Date blockedAt) {
        this.blockedAt = blockedAt;
    }

    @Override
    public String toString() {
        return "BlockedUser{" +
                "userId=" + userId +
                ", displayName='" + displayName + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                ", blockedAt=" + blockedAt +
                '}';
    }
}
