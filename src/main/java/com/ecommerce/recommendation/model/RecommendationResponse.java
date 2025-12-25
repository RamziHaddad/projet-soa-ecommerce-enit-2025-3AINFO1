package com.ecommerce.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    
    private String userId;
    private List<ScoredProduct> recommendations;
    private LocalDateTime timestamp;
    
    public Integer getTotalResults() {
        return recommendations != null ? recommendations.size() : 0;
    }
}