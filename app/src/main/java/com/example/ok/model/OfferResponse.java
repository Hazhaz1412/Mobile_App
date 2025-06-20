package com.example.ok.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OfferResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private String listingImageUrl;
    private Long buyerId;
    private String buyerName;
    private String buyerProfilePic;
    private Long sellerId;
    private String sellerName;
    private String sellerProfilePic;
    private BigDecimal offerAmount;
    private BigDecimal listingPrice;
    private String message;
    private String status; // String instead of enum for JSON parsing
    private String createdAt;
    private String updatedAt;
    private String expiresAt;
    private boolean isExpired;
    private boolean canBeAccepted;
    private boolean canBeRejected;
    private boolean canBeCountered;

    // Constructors
    public OfferResponse() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }

    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }

    public String getListingImageUrl() { return listingImageUrl; }
    public void setListingImageUrl(String listingImageUrl) { this.listingImageUrl = listingImageUrl; }

    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getBuyerProfilePic() { return buyerProfilePic; }
    public void setBuyerProfilePic(String buyerProfilePic) { this.buyerProfilePic = buyerProfilePic; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerProfilePic() { return sellerProfilePic; }
    public void setSellerProfilePic(String sellerProfilePic) { this.sellerProfilePic = sellerProfilePic; }

    public BigDecimal getOfferAmount() { return offerAmount; }
    public void setOfferAmount(BigDecimal offerAmount) { this.offerAmount = offerAmount; }

    public BigDecimal getListingPrice() { return listingPrice; }
    public void setListingPrice(BigDecimal listingPrice) { this.listingPrice = listingPrice; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public boolean isExpired() { return isExpired; }
    public void setExpired(boolean expired) { isExpired = expired; }

    public boolean isCanBeAccepted() { return canBeAccepted; }
    public void setCanBeAccepted(boolean canBeAccepted) { this.canBeAccepted = canBeAccepted; }

    public boolean isCanBeRejected() { return canBeRejected; }
    public void setCanBeRejected(boolean canBeRejected) { this.canBeRejected = canBeRejected; }

    public boolean isCanBeCountered() { return canBeCountered; }
    public void setCanBeCountered(boolean canBeCountered) { this.canBeCountered = canBeCountered; }    // Helper methods
    public String getDiscountPercentage() {
        if (listingPrice != null && listingPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = listingPrice.subtract(offerAmount);
            BigDecimal percentage = discount.divide(listingPrice, 4, RoundingMode.HALF_UP)
                                           .multiply(new BigDecimal("100"));
            return percentage.setScale(1, RoundingMode.HALF_UP).toString() + "%";
        }
        return "0%";
    }    // Convert to Offer model for adapter
    public Offer toOffer() {
        Offer offer = new Offer();
        offer.setId(this.id);
        offer.setBuyerId(this.buyerId);
        offer.setBuyerDisplayName(this.buyerName);
        offer.setSellerId(this.sellerId);
        offer.setSellerDisplayName(this.sellerName);
        offer.setListingId(this.listingId);
        offer.setListingTitle(this.listingTitle);
        offer.setAmount(this.offerAmount);
        offer.setStatus(this.status);
        offer.setMessage(this.message);
        offer.setCreatedAt(this.createdAt);
        offer.setUpdatedAt(this.updatedAt);
        
        // Create mock Listing object for adapter compatibility
        Listing listing = new Listing();
        listing.setId(this.listingId);
        listing.setTitle(this.listingTitle);
        listing.setPrice(this.listingPrice);
        listing.setPrimaryImageUrl(this.listingImageUrl);
        offer.setListing(listing);
        
        // Create mock User object for buyer
        User buyer = new User();
        buyer.setId(this.buyerId);
        buyer.setDisplayName(this.buyerName);
        buyer.setAvatarUrl(this.buyerProfilePic);
        offer.setBuyer(buyer);
        
        return offer;
    }
}
