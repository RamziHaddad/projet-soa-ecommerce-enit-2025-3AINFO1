package com.ecommerce.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {
    
    @Id
    private String id;
    private String name;
    private String description;
    private String category;
    private Double price;
    private String imageUrl;
    private Boolean active;
    
    // Analytics fields
    private Long viewCount;
    private Long salesCount;
    private Double averageRating;
    private Integer reviewCount;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}