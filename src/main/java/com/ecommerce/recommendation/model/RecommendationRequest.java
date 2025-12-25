package com.ecommerce.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequest {
    
    private String userId;
    
    @Min(1)
    private Integer limit;
    
    private String categoryFilter;
    
    private Boolean includeScoreBreakdown;
}