package com.recommendation.service;

import com.recommendation.api.dto.RecommendResponse;
import com.recommendation.api.dto.RecommendRequest;

public interface RecommendationService {
    RecommendResponse recommend(RecommendRequest request);
}
