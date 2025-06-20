package com.example.ok.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model class for user reports and moderation system
 */
public class Report {
    @SerializedName("id")
    private Long id;
    
    @SerializedName("reporterId")
    private Long reporterId;
    
    @SerializedName("reporterName")
    private String reporterName;
    
    @SerializedName("reportedUserId")
    private Long reportedUserId;
    
    @SerializedName("reportedUserName")
    private String reportedUserName;
    
    @SerializedName("reportedListingId")
    private Long reportedListingId;
    
    @SerializedName("reportedListingTitle")
    private String reportedListingTitle;
    
    @SerializedName("reportedChatRoomId")
    private Long reportedChatRoomId;
    
    @SerializedName("reportType")
    private String reportType; // USER, LISTING, CHAT
    
    @SerializedName("reason")
    private String reason;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("status")
    private String status; // PENDING, REVIEWED, RESOLVED, DISMISSED
    
    @SerializedName("reviewedBy")
    private Long reviewedBy;
    
    @SerializedName("reviewerName")
    private String reviewerName;
    
    @SerializedName("reviewNotes")
    private String reviewNotes;
    
    @SerializedName("actionTaken")
    private String actionTaken; // WARNING, TEMPORARY_BAN, PERMANENT_BAN, CONTENT_REMOVED, NONE
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("reviewedAt")
    private String reviewedAt;
    
    @SerializedName("resolvedAt")
    private String resolvedAt;
    
    // Constructors
    public Report() {}
    
    public Report(Long reporterId, Long reportedUserId, String reportType, String reason, String description) {
        this.reporterId = reporterId;
        this.reportedUserId = reportedUserId;
        this.reportType = reportType;
        this.reason = reason;
        this.description = description;
        this.status = "PENDING";
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getReporterId() {
        return reporterId;
    }
    
    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }
    
    public String getReporterName() {
        return reporterName;
    }
    
    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }
    
    public Long getReportedUserId() {
        return reportedUserId;
    }
    
    public void setReportedUserId(Long reportedUserId) {
        this.reportedUserId = reportedUserId;
    }
    
    public String getReportedUserName() {
        return reportedUserName;
    }
    
    public void setReportedUserName(String reportedUserName) {
        this.reportedUserName = reportedUserName;
    }
    
    public Long getReportedListingId() {
        return reportedListingId;
    }
    
    public void setReportedListingId(Long reportedListingId) {
        this.reportedListingId = reportedListingId;
    }
    
    public String getReportedListingTitle() {
        return reportedListingTitle;
    }
    
    public void setReportedListingTitle(String reportedListingTitle) {
        this.reportedListingTitle = reportedListingTitle;
    }
    
    public Long getReportedChatRoomId() {
        return reportedChatRoomId;
    }
    
    public void setReportedChatRoomId(Long reportedChatRoomId) {
        this.reportedChatRoomId = reportedChatRoomId;
    }
    
    public String getReportType() {
        return reportType;
    }
    
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getReviewedBy() {
        return reviewedBy;
    }
    
    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
    
    public String getReviewerName() {
        return reviewerName;
    }
    
    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }
    
    public String getReviewNotes() {
        return reviewNotes;
    }
    
    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }
    
    public String getActionTaken() {
        return actionTaken;
    }
    
    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getReviewedAt() {
        return reviewedAt;
    }
    
    public void setReviewedAt(String reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
    
    public String getResolvedAt() {
        return resolvedAt;
    }
    
    public void setResolvedAt(String resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
    
    // Utility methods
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    public boolean isResolved() {
        return "RESOLVED".equals(status);
    }
    
    public boolean isDismissed() {
        return "DISMISSED".equals(status);
    }
    
    public boolean isUserReport() {
        return "USER".equals(reportType);
    }
    
    public boolean isListingReport() {
        return "LISTING".equals(reportType);
    }
    
    public boolean isChatReport() {
        return "CHAT".equals(reportType);
    }
}
