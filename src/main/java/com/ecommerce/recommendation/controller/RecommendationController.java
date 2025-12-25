package com.ecommerce.recommendation.controller;

import com.ecommerce.recommendation.model.RecommendationRequest;
import com.ecommerce.recommendation.model.RecommendationResponse;
import com.ecommerce.recommendation.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    /**
     * Endpoint 1: Get personalized recommendations
     * POST /api/v1/recommendations/personalized
     */
    @PostMapping("/personalized")
    public ResponseEntity<RecommendationResponse> getPersonalizedRecommendations(
            @Valid @RequestBody RecommendationRequest request) {
        RecommendationResponse response = recommendationService.getRecommendations(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 2: Get user recommendations (simple)
     * GET /api/v1/recommendations/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<RecommendationResponse> getUserRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String category) {
        
        RecommendationRequest request = RecommendationRequest.builder()
            .userId(userId)
            .limit(limit)
            .categoryFilter(category)
            .build();
        
        RecommendationResponse response = recommendationService.getRecommendations(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 3: Get similar products
     * GET /api/v1/recommendations/similar/{productId}
     */
    @GetMapping("/similar/{productId}")
    public ResponseEntity<RecommendationResponse> getSimilarProducts(
            @PathVariable String productId,
            @RequestParam(defaultValue = "5") int limit) {
        
        RecommendationResponse response = recommendationService.getSimilarProducts(productId, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 4: Get trending products
     * GET /api/v1/recommendations/trending
     */
    @GetMapping("/trending")
    public ResponseEntity<RecommendationResponse> getTrendingProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String category) {
        
        RecommendationRequest request = RecommendationRequest.builder()
            .userId(null) // No user context for trending
            .limit(limit)
            .categoryFilter(category)
            .build();
        
        RecommendationResponse response = recommendationService.getRecommendations(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check
     * GET /api/v1/recommendations/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Recommendation Service is running!");
    }
}