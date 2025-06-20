package com.example.ok.model;

public class EscrowRequest {
    private Long buyerId;
    private Long reporterId;
    private String reason;
    private String adminNote;
    
    // Constructors
    public EscrowRequest() {}
    
    public EscrowRequest(Long buyerId) {
        this.buyerId = buyerId;
    }
    
    public EscrowRequest(Long reporterId, String reason) {
        this.reporterId = reporterId;
        this.reason = reason;
    }
    
    // Getters and Setters
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    
    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
}
