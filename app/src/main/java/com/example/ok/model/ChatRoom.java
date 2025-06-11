package com.example.ok.model;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatRoom {
    @SerializedName("id")
    private Long id;
    
    @SerializedName("user1Id")
    private Long user1Id;
    
    @SerializedName("user2Id")
    private Long user2Id;
    
    @SerializedName("lastMessageId")
    private Long lastMessageId;
    
    @SerializedName("lastMessageContent")
    private String lastMessageContent;
      @SerializedName("lastMessageTime")
    private String lastMessageTime;
    
    @SerializedName("unreadCount")
    private int unreadCount;
    
    @SerializedName("user1Name")
    private String user1Name;
    
    @SerializedName("user2Name")
    private String user2Name;
      @SerializedName("user1ProfilePic")
    private String user1Avatar;
    
    @SerializedName("user2ProfilePic")
    private String user2Avatar;
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("isBlocked")
    private Boolean isBlocked;
    
    @SerializedName("blockBy")
    private Long blockBy;
    
    @SerializedName("reportBy")
    private Long reportBy;
    
    @SerializedName("lastMessage")
    private String lastMessage;
    
    @SerializedName("lastMessageType")
    private String lastMessageType;
    
    @SerializedName("listingId")
    private Long listingId;
    
    @SerializedName("listingTitle")
    private String listingTitle;
    
    @SerializedName("listingImageUrl")
    private String listingImageUrl;
      // Helper methods for accessing other user info
    public Long getOtherUserId(Long currentUserId) {
        if (currentUserId == null) return null;
        return currentUserId.equals(user1Id) ? user2Id : user1Id;
    }
    
    public String getOtherUserName(Long currentUserId) {
        if (currentUserId == null) return "";
        return currentUserId.equals(user1Id) ? user2Name : user1Name;
    }
    
    public String getOtherUserAvatar(Long currentUserId) {
        if (currentUserId == null) return null;
        return currentUserId.equals(user1Id) ? user2Avatar : user1Avatar;
    }
    
    // Listing price helper
    private Long listingPrice;
    
    @SerializedName("listingPrice")
    public Long getListingPrice() {
        return listingPrice;
    }
    
    public void setListingPrice(Long listingPrice) {
        this.listingPrice = listingPrice;
    }
      // Convenience constructor for creating a new chat room
    public ChatRoom(Long user1Id, Long user2Id, Long listingId, String listingTitle) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.listingId = listingId;
        this.listingTitle = listingTitle;
        this.lastMessageTime = null; // Will be set by server
        this.unreadCount = 0;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUser1Id() {
        return user1Id;
    }
    
    public void setUser1Id(Long user1Id) {
        this.user1Id = user1Id;
    }
    
    public Long getUser2Id() {
        return user2Id;
    }
    
    public void setUser2Id(Long user2Id) {
        this.user2Id = user2Id;
    }
    
    public Long getLastMessageId() {
        return lastMessageId;
    }
    
    public void setLastMessageId(Long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }
    
    public String getLastMessageContent() {
        return lastMessageContent;
    }
    
    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }
      public String getLastMessageTime() {
        return lastMessageTime;
    }
    
    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    public String getUser1Name() {
        return user1Name;
    }
    
    public void setUser1Name(String user1Name) {
        this.user1Name = user1Name;
    }
    
    public String getUser2Name() {
        return user2Name;
    }
    
    public void setUser2Name(String user2Name) {
        this.user2Name = user2Name;
    }
    
    public String getUser1Avatar() {
        return user1Avatar;
    }
    
    public void setUser1Avatar(String user1Avatar) {
        this.user1Avatar = user1Avatar;
    }
    
    public String getUser2Avatar() {
        return user2Avatar;
    }
    
    public void setUser2Avatar(String user2Avatar) {
        this.user2Avatar = user2Avatar;
    }
    
    public Long getListingId() {
        return listingId;
    }
    
    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }
    
    public String getListingTitle() {
        return listingTitle;
    }
    
    public void setListingTitle(String listingTitle) {
        this.listingTitle = listingTitle;
    }
    
    public String getListingImageUrl() {
        return listingImageUrl;
    }
    
    public void setListingImageUrl(String listingImageUrl) {
        this.listingImageUrl = listingImageUrl;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public Boolean getIsBlocked() {
        return isBlocked;
    }
    
    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }
    
    public Long getBlockBy() {
        return blockBy;
    }
    
    public void setBlockBy(Long blockBy) {
        this.blockBy = blockBy;
    }
    
    public Long getReportBy() {
        return reportBy;
    }
    
    public void setReportBy(Long reportBy) {
        this.reportBy = reportBy;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public String getLastMessageType() {
        return lastMessageType;
    }
    
    public void setLastMessageType(String lastMessageType) {
        this.lastMessageType = lastMessageType;    }
    
    // Helper methods
    
    public Date getLastMessageDate() {
        if (lastMessageTime != null && !lastMessageTime.isEmpty()) {
            try {
                // Parse ISO datetime string (2025-06-10T12:56:44.272811)
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
                return isoFormat.parse(lastMessageTime);
            } catch (Exception e) {
                try {
                    // Try without microseconds
                    SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    return simpleFormat.parse(lastMessageTime);
                } catch (Exception e2) {
                    return new Date();                }
            }
        }
        return new Date();
    }
}
