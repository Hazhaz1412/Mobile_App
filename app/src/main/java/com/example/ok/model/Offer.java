package com.example.ok.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Offer implements Serializable {
    private static final long serialVersionUID = 1L;
      // Enum for offer status
    public enum OfferStatus {
        PENDING, ACCEPTED, REJECTED, WITHDRAWN, COUNTERED, EXPIRED, COMPLETED
    }
    
    private Long id;
    private Long buyerId;
    private String buyerDisplayName;
    private Long sellerId;
    private String sellerDisplayName;
    private Long listingId;
    private String listingTitle;    private BigDecimal amount;
    private String status; // String version of status for JSON parsing
    private String message;
    private String createdAt;
    private String updatedAt;
    private boolean hasPaidTransaction; // Flag to check if offer has been paid
    
    // Additional properties that adapters expect
    private Listing listing; // For convenience - will be null from API
    private User buyer; // For convenience - will be null from API

    // Constructors
    public Offer() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

    public String getBuyerDisplayName() { return buyerDisplayName; }
    public void setBuyerDisplayName(String buyerDisplayName) { this.buyerDisplayName = buyerDisplayName; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public String getSellerDisplayName() { return sellerDisplayName; }
    public void setSellerDisplayName(String sellerDisplayName) { this.sellerDisplayName = sellerDisplayName; }

    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }

    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isHasPaidTransaction() { return hasPaidTransaction; }
    public void setHasPaidTransaction(boolean hasPaidTransaction) { this.hasPaidTransaction = hasPaidTransaction; }
    
    // Additional getters for compatibility with existing adapters
    public Listing getListing() { return listing; }
    public void setListing(Listing listing) { this.listing = listing; }
    
    public User getBuyer() { return buyer; }
    public void setBuyer(User buyer) { this.buyer = buyer; }
    
    // Compatibility methods
    public BigDecimal getOfferAmount() { return amount; }
    public void setOfferAmount(BigDecimal offerAmount) { this.amount = offerAmount; }
    
    // Helper method to get status as enum
    public OfferStatus getStatusEnum() {
        if (status == null) return OfferStatus.PENDING;
        try {
            return OfferStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OfferStatus.PENDING;
        }
    }
}
