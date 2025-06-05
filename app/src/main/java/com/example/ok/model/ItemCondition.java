package com.example.ok.model;

public class ItemCondition {
    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;

    // Constructors
    public ItemCondition() {}

    public ItemCondition(Long id, String name, String description, Integer displayOrder) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}