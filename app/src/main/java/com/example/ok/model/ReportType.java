package com.example.ok.model;

/**
 * Enum class for report types in the moderation system
 */
public enum ReportType {
    USER("Người dùng"),
    LISTING("Tin đăng"),
    CHAT("Trò chuyện");
    
    private final String displayName;
    
    ReportType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
