package com.recommendation.ann;

import com.recommendation.domain.Candidate;
import java.util.List;
import java.util.Map;

public interface ANNClient {
    List<Candidate> getTopN(float[] userEmbedding, int n, Map<String, Object> filters);
}
