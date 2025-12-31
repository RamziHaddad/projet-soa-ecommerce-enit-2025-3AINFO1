package com.recommendation.integration.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recommendation.integration.dto.FeedbackEventDTO;
import com.recommendation.ml.FeatureEngineer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Listener pour les événements du Feedback Service
 * Topics: feedback.events
 */
@Slf4j
@Service
public class FeedbackEventListener {
    
    private final FeatureEngineer featureEngineer;
    private final ObjectMapper objectMapper;
    
    public FeedbackEventListener(FeatureEngineer featureEngineer, ObjectMapper objectMapper) {
        this.featureEngineer = featureEngineer;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "feedback.events", groupId = "recommendation-service")
    public void handleFeedbackEvent(String message) {
        try {
            FeedbackEventDTO event = objectMapper.readValue(message, FeedbackEventDTO.class);
            log.info("Received feedback event: productId={}, userId={}, rating={}", 
                event.getProductId(), event.getUserId(), event.getRating());
            
            // Enregistrer le feedback/rating pour affiner les features
            featureEngineer.recordFeedback(
                String.valueOf(event.getUserId()),
                event.getProductId(),
                event.getRating(),
                event.getComment()
            );
            log.info("Feedback recorded for product: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Error processing feedback event", e);
        }
    }
}
