package com.example.ok.model;

/**
 * Enum class for report reasons in the moderation system
 */
public enum ReportReason {
    SCAM_FRAUD("Lừa đảo/Gian lận"),
    INAPPROPRIATE_CONTENT("Nội dung không phù hợp"),
    SPAM("Spam/Quảng cáo"),
    HARASSMENT("Quấy rối"),
    FAKE_PRODUCT("Hàng giả/Không như mô tả"),
    OFFENSIVE_LANGUAGE("Ngôn từ xúc phạm"),
    FAKE_PROFILE("Tài khoản giả mạo"),
    PRICE_MANIPULATION("Thao túng giá cả"),
    DUPLICATE_LISTING("Tin đăng trùng lặp"),
    OTHER("Khác");
    
    private final String displayName;
    
    ReportReason(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static ReportReason[] getUserReportReasons() {
        return new ReportReason[]{
            SCAM_FRAUD, INAPPROPRIATE_CONTENT, SPAM, 
            HARASSMENT, OFFENSIVE_LANGUAGE, FAKE_PROFILE, OTHER
        };
    }
    
    public static ReportReason[] getListingReportReasons() {
        return new ReportReason[]{
            SCAM_FRAUD, INAPPROPRIATE_CONTENT, SPAM, 
            FAKE_PRODUCT, PRICE_MANIPULATION, DUPLICATE_LISTING, OTHER
        };
    }
    
    public static ReportReason[] getChatReportReasons() {
        return new ReportReason[]{
            SCAM_FRAUD, INAPPROPRIATE_CONTENT, SPAM, 
            HARASSMENT, OFFENSIVE_LANGUAGE, OTHER
        };
    }
}
