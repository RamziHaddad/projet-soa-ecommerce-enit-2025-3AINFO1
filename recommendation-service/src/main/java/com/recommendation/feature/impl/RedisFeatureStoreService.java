package com.recommendation.feature.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recommendation.feature.FeatureStoreService;
import com.recommendation.feature.UserEmbeddingEntity;
import com.recommendation.feature.UserEmbeddingRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RedisFeatureStoreService implements FeatureStoreService {
    private final StringRedisTemplate redis;
    private final UserEmbeddingRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    public RedisFeatureStoreService(StringRedisTemplate redis, UserEmbeddingRepository repo) {
        this.redis = redis;
        this.repo = repo;
    }

    @Override
    public float[] getUserEmbedding(String userId) {
        String key = "user_embed:" + userId;
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                UserEmbeddingEntity ent = repo.findById(userId).orElse(null);
                if (ent != null) json = ent.getEmbeddingJson();
            }
            if (json != null) {
                try {
                    List<Double> vals = mapper.readValue(json.getBytes(StandardCharsets.UTF_8), new TypeReference<List<Double>>(){});
                    float[] out = new float[vals.size()];
                    for (int i = 0; i < vals.size(); i++) out[i] = vals.get(i).floatValue();
                    return out;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // Redis connection failure—fall back to default
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Redis unavailable, using default embedding", e);
        }
        float[] e = new float[128];
        for (int i = 0; i < e.length; i++) e[i] = 0.01f * (i + 1);
        return e;
    }
}
