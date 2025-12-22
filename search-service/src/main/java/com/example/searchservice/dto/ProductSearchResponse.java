package com.example.searchservice.dto;

import com.example.searchservice.model.ProductIndex;

public class ProductSearchResponse {
    private String id;
    private String name;
    private String description;
    private Double price;
    private String category;

    public ProductSearchResponse(ProductIndex productIndex) {
        this.id = productIndex.getId();
        this.name = productIndex.getName();
        this.description = productIndex.getDescription();
        this.price = productIndex.getPrice();
        this.category = productIndex.getCategory();
    }

    // Getters et setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
