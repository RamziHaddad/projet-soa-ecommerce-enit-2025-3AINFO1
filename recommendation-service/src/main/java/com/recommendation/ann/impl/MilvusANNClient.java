package com.recommendation.ann.impl;

import com.recommendation.ann.ANNClient;
import com.recommendation.domain.Candidate;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;

import java.util.*;

public class MilvusANNClient implements ANNClient {
    private final String host;
    private final int port;
    // Placeholder fields for future Milvus schema
    private final String collection = "product_embeddings";
    private final String vectorField = "embedding";

    public MilvusANNClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public List<Candidate> getTopN(float[] userEmbedding, int n, Map<String, Object> filters) {
        // TODO: Implement actual Milvus search once schema is finalized
        // For now, always fallback to stub to keep service operational.
        return new StubANNClient().getTopN(userEmbedding, n, filters);
    }

        // ensureCollection removed until schema/version is finalized

    private List<Float> toFloatList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float v : arr) list.add(v);
        return list;
    }
}
