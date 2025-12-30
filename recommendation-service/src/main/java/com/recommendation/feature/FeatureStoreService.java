package com.recommendation.feature;

public interface FeatureStoreService {
    float[] getUserEmbedding(String userId);
}
