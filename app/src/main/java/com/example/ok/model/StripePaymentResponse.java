package com.example.ok.model;

public class StripePaymentResponse {
    private boolean success;
    private String message;
    private String sessionId;
    private String checkoutUrl;
    private Long paymentId;
    private String status;
    private Double amount;
    private String currency;
    private Long expiresAt;
    private String errorCode;
    private String transactionId;

    public StripePaymentResponse() {}

    public StripePaymentResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public String toString() {
        return "StripePaymentResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", checkoutUrl='" + checkoutUrl + '\'' +
                ", paymentId=" + paymentId +
                ", status='" + status + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", expiresAt=" + expiresAt +
                ", errorCode='" + errorCode + '\'' +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }
}
