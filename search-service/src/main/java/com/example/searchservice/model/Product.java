// src/main/java/com/example/searchservice/model/Product.java
package com.example.searchservice.model;

public class Product {

    private String id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private boolean isIndexed;
    private String lastUpdateTime; // Format ISO comme "2023-01-01T00:00:00"

    public Product() {}

    public Product(String id, String name, String description, Double price, String category, boolean isIndexed, String lastUpdateTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.isIndexed = isIndexed;
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isIndexed() {
        return isIndexed;
    }

    public void setIndexed(boolean indexed) {
        isIndexed = indexed;
    }

    public String getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}