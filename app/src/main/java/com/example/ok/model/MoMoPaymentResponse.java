package com.example.ok.model;

public class MoMoPaymentResponse {
    private boolean success;
    private String message;
    private Payment payment;
    private String paymentUrl;
    private String qrCodeUrl;
    private String deeplink;
    private String deeplinkMiniApp;
    private String qrCode;
    
    // Constructors
    public MoMoPaymentResponse() {}
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    
    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }
    
    public String getDeeplink() { return deeplink; }
    public void setDeeplink(String deeplink) { this.deeplink = deeplink; }
    
    public String getDeeplinkMiniApp() { return deeplinkMiniApp; }
    public void setDeeplinkMiniApp(String deeplinkMiniApp) { this.deeplinkMiniApp = deeplinkMiniApp; }
    
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    
    // Helper method để lấy payment ID
    public Long getPaymentId() {
        return payment != null ? payment.getId() : null;
    }
}
