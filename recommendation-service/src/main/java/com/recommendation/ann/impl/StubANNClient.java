package com.recommendation.ann.impl;

import com.recommendation.ann.ANNClient;
import com.recommendation.domain.Candidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StubANNClient implements ANNClient {
    @Override
    public List<Candidate> getTopN(float[] userEmbedding, int n, Map<String, Object> filters) {
        // Stub: return dummy candidates; replace with Milvus/ES implementation
        List<Candidate> list = new ArrayList<>();
        for (int i = 1; i <= Math.min(n, 50); i++) {
            list.add(new Candidate("product-" + i, 1.0 / i));
        }
        return list;
    }
}
