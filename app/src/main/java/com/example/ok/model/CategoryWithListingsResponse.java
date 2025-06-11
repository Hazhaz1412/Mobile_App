package com.example.ok.model;

import java.util.List;

public class CategoryWithListingsResponse {
    private Long categoryId;
    private String categoryName;
    private List<Listing> listings;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public List<Listing> getListings() { return listings; }
    public void setListings(List<Listing> listings) { this.listings = listings; }
}
