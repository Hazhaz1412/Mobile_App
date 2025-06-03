// GoogleAuthRequest.java
package com.example.ok.model;

public class GoogleAuthRequest {
    private String idToken;
    private String email;
    private String displayName;

    public GoogleAuthRequest(String idToken, String email, String displayName) {
        this.idToken = idToken;
        this.email = email;
        this.displayName = displayName;
    }

    // Getters and setters
    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}