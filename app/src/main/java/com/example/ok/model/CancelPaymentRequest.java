package com.example.ok.model;

public class CancelPaymentRequest {
    private Long userId;
    private String reason;
    private String userRole; // "BUYER" or "SELLER"
    
    public CancelPaymentRequest() {}
    
    public CancelPaymentRequest(Long userId, String reason, String userRole) {
        this.userId = userId;
        this.reason = reason;
        this.userRole = userRole;
    }
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
}
