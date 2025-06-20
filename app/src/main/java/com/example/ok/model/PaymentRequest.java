package com.example.ok.model;

public class PaymentRequest {
    private Long listingId;
    private String paymentMethodType;
    private Double amount;
    private String description;
    private Long buyerId;
    private Long sellerId;
    
    // For MoMo payment
    private String phoneNumber;
    
    // For card payment
    private String cardNumber;
    private String expiryDate;
    private String cvv;
    private String holderName;
      // Additional fields for backend compatibility
    private String currency = "VND";
    private String language = "vi";
    
    // Escrow protection
    private Boolean useEscrow = true; // Mặc định sử dụng bảo vệ
    
    public PaymentRequest() {}
    
    public PaymentRequest(Long listingId, String paymentMethodType, Double amount) {
        this.listingId = listingId;
        this.paymentMethodType = paymentMethodType;
        this.amount = amount;
    }
    
    // Getters and Setters
    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }
    
    public String getPaymentMethodType() { return paymentMethodType; }
    public void setPaymentMethodType(String paymentMethodType) { this.paymentMethodType = paymentMethodType; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
      public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public Boolean getUseEscrow() { return useEscrow; }
    public void setUseEscrow(Boolean useEscrow) { this.useEscrow = useEscrow; }
}
