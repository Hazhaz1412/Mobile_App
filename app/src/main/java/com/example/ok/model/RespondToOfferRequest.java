package com.example.ok.model;

import java.math.BigDecimal;

public class RespondToOfferRequest {
    private String action; // ACCEPT, REJECT, COUNTER
    private BigDecimal counterAmount; // only for COUNTER action
    private String message;

    // Constructors
    public RespondToOfferRequest() {}

    public RespondToOfferRequest(String action, String message) {
        this.action = action;
        this.message = message;
    }

    public RespondToOfferRequest(String action, BigDecimal counterAmount, String message) {
        this.action = action;
        this.counterAmount = counterAmount;
        this.message = message;
    }

    // Getters and Setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public BigDecimal getCounterAmount() { return counterAmount; }
    public void setCounterAmount(BigDecimal counterAmount) { this.counterAmount = counterAmount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
