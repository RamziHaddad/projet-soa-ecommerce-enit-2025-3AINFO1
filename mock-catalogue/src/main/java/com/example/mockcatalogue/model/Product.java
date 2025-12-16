package com.example.mockcatalogue.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Product {

    @Id
    private String id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private boolean isIndexed = false;
    private LocalDateTime lastUpdateTime = LocalDateTime.now();

    // Constructeurs, getters, setters
    public Product() {}

    public Product(String id, String name, String description, Double price, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
    }

    // Getters et setters pour tous les champs...
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
    public boolean isIndexed() { return isIndexed; }
    public void setIndexed(boolean indexed) { isIndexed = indexed; }
    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
}