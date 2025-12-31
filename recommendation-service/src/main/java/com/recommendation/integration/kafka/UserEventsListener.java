package com.recommendation.integration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recommendation.entity.UserInteraction;
import com.recommendation.feature.UserEmbeddingEntity;
import com.recommendation.feature.UserEmbeddingRepository;
import com.recommendation.ml.TrendingDetectorService;
import com.recommendation.repository.UserInteractionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Listener for user.events integrated with the recommendation service.
 */
@Slf4j
@Service
public class UserEventsListener {
    private final UserEmbeddingRepository repo;
    private final StringRedisTemplate redis;
    private final UserInteractionRepository interactionRepository;
    private final TrendingDetectorService trendingDetector;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserEventsListener(UserEmbeddingRepository repo,
                              StringRedisTemplate redis,
                              UserInteractionRepository interactionRepository,
                              TrendingDetectorService trendingDetector) {
        this.repo = repo;
        this.redis = redis;
        this.interactionRepository = interactionRepository;
        this.trendingDetector = trendingDetector;
    }

    @KafkaListener(topics = "user.events", groupId = "recommendation-service")
    public void handle(String message) {
        try {
            JsonNode event = mapper.readTree(message);
            String userId = event.get("userId").asText();
            String eventType = event.get("event").asText();
            String productId = event.has("productId") ? event.get("productId").asText() : null;
            long timestamp = event.has("timestamp") ? event.get("timestamp").asLong() : System.currentTimeMillis();

            // Persist interaction for training
            if (productId != null) {
                UserInteraction interaction = new UserInteraction();
                interaction.setUserId(userId);
                interaction.setProductId(productId);
                interaction.setEventType(eventType);
                interaction.setTimestamp(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
                interactionRepository.save(interaction);

                // Record trending signals
                if ("product_click".equals(eventType) || "product_view".equals(eventType)) {
                    trendingDetector.recordProductView(productId);
                }
            }

            // Update user embedding (stubbed)
            UserEmbeddingEntity entity = repo.findById(userId).orElseGet(() -> {
                UserEmbeddingEntity e = new UserEmbeddingEntity();
                e.setUserId(userId);
                return e;
            });
            float[] embedding = generateUserEmbedding(userId);
            List<Double> embeddingList = new ArrayList<>();
            for (float v : embedding) embeddingList.add((double) v);
            entity.setEmbeddingJson(mapper.writeValueAsString(embeddingList));
            repo.save(entity);
            redis.opsForValue().set("user_embed:" + userId, mapper.writeValueAsString(embeddingList));
            log.info("user.events processed: type={} userId={} productId={}", eventType, userId, productId);
        } catch (Exception e) {
            log.error("Error processing user event", e);
        }
    }

    private float[] generateUserEmbedding(String userId) {
        float[] embedding = new float[128];
        int hash = userId.hashCode();
        for (int i = 0; i < 128; i++) {
            embedding[i] = (float) Math.sin((hash + i) * 0.01) * 0.5f + 0.5f;
        }
        return embedding;
    }
}