package com.example.ok.model;

public class CreateRatingRequest {
    private Long transactionId;
    private Long ratedUserId;
    private int rating;
    private String comment;
    
    public CreateRatingRequest() {}
    
    public CreateRatingRequest(Long transactionId, Long ratedUserId, int rating, String comment) {
        this.transactionId = transactionId;
        this.ratedUserId = ratedUserId;
        this.rating = rating;
        this.comment = comment;
    }
    
    // Getters and Setters
    public Long getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
    
    public Long getRatedUserId() {
        return ratedUserId;
    }
    
    public void setRatedUserId(Long ratedUserId) {
        this.ratedUserId = ratedUserId;
    }
    
    public int getRating() {
        return rating;
    }
    
    public void setRating(int rating) {
        this.rating = rating;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
}
