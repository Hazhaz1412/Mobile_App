package com.example.ok.model;

import java.time.LocalDateTime;

public class EscrowInfo {
    private Long paymentId;
    private Boolean useEscrow;
    private String escrowStatus;
    private String holdUntil;
    private String releasedAt;
    private String disputeReportedAt;
    private Long disputeReporterId;
    private String disputeReason;
    private long daysRemaining;
    
    // Constructors
    public EscrowInfo() {}
    
    // Getters and Setters
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    
    public Boolean getUseEscrow() { return useEscrow; }
    public void setUseEscrow(Boolean useEscrow) { this.useEscrow = useEscrow; }
    
    public String getEscrowStatus() { return escrowStatus; }
    public void setEscrowStatus(String escrowStatus) { this.escrowStatus = escrowStatus; }
    
    public String getHoldUntil() { return holdUntil; }
    public void setHoldUntil(String holdUntil) { this.holdUntil = holdUntil; }
    
    public String getReleasedAt() { return releasedAt; }
    public void setReleasedAt(String releasedAt) { this.releasedAt = releasedAt; }
    
    public String getDisputeReportedAt() { return disputeReportedAt; }
    public void setDisputeReportedAt(String disputeReportedAt) { this.disputeReportedAt = disputeReportedAt; }
    
    public Long getDisputeReporterId() { return disputeReporterId; }
    public void setDisputeReporterId(Long disputeReporterId) { this.disputeReporterId = disputeReporterId; }
    
    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }
    
    public long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }
    
    // Helper methods
    public boolean isHolding() {
        return "HOLDING".equals(escrowStatus);
    }
    
    public boolean isDisputed() {
        return "DISPUTED".equals(escrowStatus);
    }
    
    public boolean isReleased() {
        return "RELEASED".equals(escrowStatus);
    }
    
    public boolean isRefunded() {
        return "REFUNDED".equals(escrowStatus);
    }
    
    public boolean canRelease() {
        return isHolding() && daysRemaining > 0;
    }
    
    public boolean canReportDispute() {
        return isHolding() && daysRemaining > 0;
    }
}
