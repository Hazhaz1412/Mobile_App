package com.example.ok.model;

public class WithdrawOfferRequest {
    private Long offerId;
    
    public WithdrawOfferRequest() {}
    
    public WithdrawOfferRequest(Long offerId) {
        this.offerId = offerId;
    }
    
    public Long getOfferId() {
        return offerId;
    }
    
    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }
}
