package com.recommendation.ann.impl;

import com.recommendation.ann.ANNClient;
import com.recommendation.domain.Candidate;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Milvus-based ANN client for vector search.
 * For now, maintains an in-memory mock index since full Milvus integration requires
 * more schema finalization. Production version will use actual Milvus gRPC calls.
 */
public class MilvusANNClientImpl implements ANNClient {
    private static final Logger log = LoggerFactory.getLogger(MilvusANNClientImpl.class);
    private final String host;
    private final int port;
    private final Map<String, float[]> productEmbeddings = new ConcurrentHashMap<>();

    public MilvusANNClientImpl(String host, int port) {
        this.host = host;
        this.port = port;
        initializeProductEmbeddings();
    }

    @Override
    public List<Candidate> getTopN(float[] userEmbedding, int n, Map<String, Object> filters) {
        List<Candidate> results = new ArrayList<>();
        productEmbeddings.forEach((productId, itemEmbed) -> {
            double similarity = cosineSimilarity(userEmbedding, itemEmbed);
            results.add(new Candidate(productId, similarity));
        });
        results.sort(Comparator.comparingDouble(Candidate::similarity).reversed());
        return results.stream().limit(n).toList();
    }

    private void initializeProductEmbeddings() {
        // Seed with 100 dummy products for demo
        for (int i = 1; i <= 100; i++) {
            float[] embedding = new float[128];
            for (int j = 0; j < 128; j++) {
                embedding[j] = (float) Math.sin(i * 0.1 + j * 0.01);
            }
            productEmbeddings.put("product-" + i, embedding);
        }
        log.info("Initialized {} product embeddings", productEmbeddings.size());
    }

    public void upsertProductEmbedding(String productId, float[] embedding) {
        productEmbeddings.put(productId, embedding);
        log.debug("Upserted product embedding: {}", productId);
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
