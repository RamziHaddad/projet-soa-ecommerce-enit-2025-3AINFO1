package com.recommendation.api.dto;

import java.util.List;

public record RecommendResponse(List<RecommendationItem> recommendations) {
    public static record RecommendationItem(String productId, double score) {}
}
