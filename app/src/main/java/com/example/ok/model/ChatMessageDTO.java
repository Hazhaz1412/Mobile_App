package com.example.ok.model;

public class ChatMessageDTO {
    private Long roomId;
    private Long senderId;
    private String content;
    private String type; // "TEXT" hoặc "IMAGE"

    public ChatMessageDTO(Long roomId, Long senderId, String content, String type) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.type = type;
    }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
