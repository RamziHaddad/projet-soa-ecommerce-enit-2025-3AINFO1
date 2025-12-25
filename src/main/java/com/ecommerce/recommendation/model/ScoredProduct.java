package com.ecommerce.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoredProduct {
    
    private String productId;
    private String productName;
    private String category;
    private Double price;
    private String imageUrl;
    private Double score;
    private Map<String, Double> scoreBreakdown;
}