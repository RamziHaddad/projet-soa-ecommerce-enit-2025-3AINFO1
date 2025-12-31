package com.recommendation.feature;

import jakarta.persistence.*;

@Entity
@Table(name = "product_embeddings")
public class ProductEmbeddingEntity {
    @Id
    @Column(name = "product_id", nullable = false)
    private String productId;

    @Lob
    @Column(name = "embedding_json", nullable = false)
    private String embeddingJson;

    @Column(name = "category")
    private String category;

    @Column(name = "price")
    private Double price;

    @Column(name = "popularity")
    private Double popularity;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getEmbeddingJson() { return embeddingJson; }
    public void setEmbeddingJson(String embeddingJson) { this.embeddingJson = embeddingJson; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getPopularity() { return popularity; }
    public void setPopularity(Double popularity) { this.popularity = popularity; }
}
