// filepath: com.example.ok.model.RegisterRequest.java
package com.example.ok.model;

public class RegisterRequest {
    private String email;
    private String password;
    private String displayName;

    // Constructors
    public RegisterRequest() {}

    public RegisterRequest(String email, String password, String displayName) {
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}