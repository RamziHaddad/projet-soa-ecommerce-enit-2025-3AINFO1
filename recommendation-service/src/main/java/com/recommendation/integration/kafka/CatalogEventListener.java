package com.recommendation.integration.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recommendation.integration.dto.CatalogEventDTO;
import com.recommendation.ml.FeatureEngineer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Listener pour les événements du Catalog Service
 * Topics: catalog.events
 */
@Slf4j
@Service
public class CatalogEventListener {
    
    private final FeatureEngineer featureEngineer;
    private final ObjectMapper objectMapper;
    
    public CatalogEventListener(FeatureEngineer featureEngineer, ObjectMapper objectMapper) {
        this.featureEngineer = featureEngineer;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "catalog.events", groupId = "recommendation-service")
    public void handleCatalogEvent(String message) {
        try {
            CatalogEventDTO event = objectMapper.readValue(message, CatalogEventDTO.class);
            log.info("Received catalog event: product={}, type={}", event.getProductId(), event.getEventType());
            
            // Mettre à jour les features du produit dans la feature store
            switch (event.getEventType()) {
                case "CREATED":
                case "UPDATED":
                    // Extraire les features du produit
                    featureEngineer.extractProductFeatures(
                        event.getProductId(),
                        event.getName(),
                        event.getDescription(),
                        event.getPrice()
                    );
                    log.info("Product features updated: {}", event.getProductId());
                    break;
                case "DELETED":
                    log.info("Product deleted: {}", event.getProductId());
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing catalog event", e);
        }
    }
}
