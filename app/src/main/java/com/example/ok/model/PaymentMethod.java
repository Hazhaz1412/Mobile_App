package com.example.ok.model;

public class PaymentMethod {
    public static final String MOMO = "MOMO";
    public static final String VISA = "VISA";
    public static final String MASTERCARD = "MASTERCARD";
    public static final String STRIPE = "STRIPE";
    public static final String CASH = "CASH";
    
    private Long id;
    private String type; // MOMO, VISA, MASTERCARD, CASH
    private String displayName;
    private String maskedNumber; // For cards: **** **** **** 1234, For MoMo: 09****567
    private String iconResource;
    private boolean isDefault;
    private boolean isActive;
    
    // For MoMo
    private String phoneNumber;
    
    // For Cards
    private String cardNumber;
    private String expiryDate;
    private String holderName;
    
    public PaymentMethod() {}
    
    public PaymentMethod(String type, String displayName, String maskedNumber) {
        this.type = type;
        this.displayName = displayName;
        this.maskedNumber = maskedNumber;
        this.isActive = true;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getMaskedNumber() { return maskedNumber; }
    public void setMaskedNumber(String maskedNumber) { this.maskedNumber = maskedNumber; }
    
    public String getIconResource() { return iconResource; }
    public void setIconResource(String iconResource) { this.iconResource = iconResource; }
    
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
}
