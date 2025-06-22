package com.example.ok.model;

public class VerifyCodeRequest {
    private String email;
    private String verificationCode;
    private String action; // "DEACTIVATE" hoặc "DELETE"
    
    public VerifyCodeRequest(String email, String verificationCode, String action) {
        this.email = email;
        this.verificationCode = verificationCode;
        this.action = action;
    }
    
    // Getters and setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getVerificationCode() {
        return verificationCode;
    }
    
    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
}
