package com.example.ok.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class UserProfileResponse {
    private boolean success;
    private String message;
    private User data; // Đối tượng User cho tương thích ngược
    
    // Trường trực tiếp từ API
    @SerializedName("userId")
    private Long userId;
    
    @SerializedName("displayName")
    private String displayName;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("bio")
    private String bio;
    
    @SerializedName("contactInfo")
    private String contactInfo;
    
    @SerializedName("profilePictureUrl")
    private String profilePictureUrl;
    
    @SerializedName("ratingAvg")
    private BigDecimal ratingAvg;
    
    @SerializedName("ratingCount")
    private Integer ratingCount;
    
    // Constructor
    public UserProfileResponse() {}
    
    // Getters
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public User getData() {
        // Nếu data null, tạo User từ trường trực tiếp
        if (data == null) {
            data = new User();
            data.setId(userId);
            data.setDisplayName(displayName);
            data.setEmail(email);
            data.setBio(bio);
            data.setContactInfo(contactInfo);
            data.setAvatarUrl(profilePictureUrl);
            // Set các trường khác nếu cần
        }
        return data;
    }
    
    // Setters  
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public void setData(User data) {
        this.data = data;
    }
    
    // Legacy getters
    public Long getUserId() {
        return userId != null ? userId : (data != null ? data.getId() : null);
    }
    
    public String getDisplayName() {
        return displayName != null ? displayName : (data != null ? data.getDisplayName() : null);
    }
    
    public String getEmail() {
        return email != null ? email : (data != null ? data.getEmail() : null);
    }
    
    public String getProfilePictureUrl() {
        return profilePictureUrl != null ? profilePictureUrl : (data != null ? data.getAvatarUrl() : null);
    }
    
    public String getBio() {
        return bio != null ? bio : (data != null ? data.getBio() : null);
    }
    
    public String getContactInfo() {
        return contactInfo != null ? contactInfo : (data != null ? data.getContactInfo() : null);
    }
    
    public BigDecimal getRatingAvg() {
        return ratingAvg != null ? ratingAvg : BigDecimal.valueOf(0);
    }
    
    public Integer getRatingCount() {
        return ratingCount != null ? ratingCount : 0;
    }
    
    // Legacy setters
    public void setUserId(Long userId) {
        this.userId = userId;
        if (data != null) data.setId(userId);
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        if (data != null) data.setDisplayName(displayName);
    }
    
    public void setEmail(String email) {
        this.email = email;
        if (data != null) data.setEmail(email);
    }
    
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
        if (data != null) data.setAvatarUrl(profilePictureUrl);
    }
    
    public void setBio(String bio) {
        this.bio = bio;
        if (data != null) data.setBio(bio);
    }
    
    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
        if (data != null) data.setContactInfo(contactInfo);
    }
    
    public void setRatingAvg(BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }
    
    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }
}
