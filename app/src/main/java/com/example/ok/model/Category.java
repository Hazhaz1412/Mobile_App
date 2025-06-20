package com.example.ok.model;

public class Category {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private Boolean isActive;    // Constructors
    public Category() {}

    public Category(Long id, String name) {
        this.id = id;
        this.name = name;
        this.isActive = true;
    }

    public Category(Long id, String name, String description, String iconUrl, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}