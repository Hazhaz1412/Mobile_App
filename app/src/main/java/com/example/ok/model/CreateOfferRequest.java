package com.example.ok.model;

import java.math.BigDecimal;

public class CreateOfferRequest {
    private Long listingId;
    private BigDecimal offerAmount;
    private String message;

    // Constructors
    public CreateOfferRequest() {}

    public CreateOfferRequest(Long listingId, BigDecimal offerAmount, String message) {
        this.listingId = listingId;
        this.offerAmount = offerAmount;
        this.message = message;
    }

    // Getters and Setters
    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }

    public BigDecimal getOfferAmount() { return offerAmount; }
    public void setOfferAmount(BigDecimal offerAmount) { this.offerAmount = offerAmount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
