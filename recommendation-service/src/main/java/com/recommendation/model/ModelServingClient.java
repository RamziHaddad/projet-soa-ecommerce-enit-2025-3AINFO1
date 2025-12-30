package com.recommendation.model;

import com.recommendation.domain.Candidate;
import com.recommendation.domain.Recommendation;
import java.util.List;
import java.util.Map;

public interface ModelServingClient {
    List<Recommendation> score(String userId, float[] userEmbedding, List<Candidate> candidates, Map<String, Object> context, int k);
}
