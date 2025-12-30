package com.recommendation.api.dto;

import java.util.Map;

public record RecommendRequest(String userId, Map<String, Object> context, Integer limit) {}
