package com.example.ok.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

// Enhanced model for chat messages
public class ChatMessage {
    @SerializedName("id")
    private Long id;
      @SerializedName("chatRoomId")
    private Long roomId;
    
    @SerializedName("senderId")
    private Long senderId;
    
    @SerializedName("receiverId")
    private Long receiverId;
    
    @SerializedName("senderName")
    private String senderName;
    
    @SerializedName("senderProfilePic")
    private String senderProfilePic;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("type")
    private String type; // TEXT, IMAGE, ...
    
    @SerializedName("imageUrl")
    private String imageUrl;
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("timestamp")
    private Long timestamp; // Keep for backward compatibility
    
    @SerializedName("isRead")
    private boolean read;
    
    // Constructor for sending new messages
    public ChatMessage(Long roomId, Long senderId, Long receiverId, String content, String type) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }
    
    // Empty constructor for Gson
    public ChatMessage() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
    
    public Long getSenderId() {
        return senderId;
    }
    
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
    
    public Long getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
      public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getSenderProfilePic() {
        return senderProfilePic;
    }
    
    public void setSenderProfilePic(String senderProfilePic) {
        this.senderProfilePic = senderProfilePic;
    }
    
    public boolean isRead() {
        return read;
    }
    
    public void setRead(boolean read) {
        this.read = read;
    }
    
    // Convenience methods
    public boolean isImage() {
        return "IMAGE".equals(type);
    }
    
    public boolean isText() {
        return "TEXT".equals(type);
    }
      public Date getDateFromTimestamp() {
        // Try timestamp first (for backward compatibility)
        if (timestamp != null && timestamp > 0) {
            return new Date(timestamp);
        }
          // Fall back to createdAt string parsing
        if (createdAt != null && !createdAt.isEmpty()) {
            try {
                // Parse ISO datetime string: "2025-06-10T12:56:44.272811"
                // Use SimpleDateFormat for API level compatibility
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.getDefault());
                return sdf.parse(createdAt);
            } catch (Exception e) {
                try {
                    // Try alternative format without microseconds
                    java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                    return sdf2.parse(createdAt);
                } catch (Exception e2) {
                    // If parsing fails, return current time
                    return new Date();
                }
            }
        }
          // If both are null, return current time
        return new Date();
    }
    
    // Helper method to get ID safely
    public long getIdSafely() {
        return id != null ? id : 0L;
    }
}
