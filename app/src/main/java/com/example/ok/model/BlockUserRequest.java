package com.example.ok.model;

public class BlockUserRequest {
    private String reason;
    
    public BlockUserRequest() {}
    
    public BlockUserRequest(String reason) {
        this.reason = reason;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}
