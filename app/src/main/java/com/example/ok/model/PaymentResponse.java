package com.example.ok.model;

public class PaymentResponse {
    private Boolean success;
    private String message;
    private Payment payment;
    private String paymentUrl; // For redirect-based payments like MoMo
    private String qrCode; // For QR code payments
    
    public PaymentResponse() {}
    
    public PaymentResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Getters and Setters
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    
    // Helper methods
    public boolean isSuccess() { return success != null && success; }
}
