package com.recommendation.integration.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recommendation.integration.dto.InventoryEventDTO;
import com.recommendation.ml.FeatureEngineer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Listener pour les événements du Inventory Service
 * Topics: inventory.events
 */
@Slf4j
@Service
public class InventoryEventListener {
    
    private final FeatureEngineer featureEngineer;
    private final ObjectMapper objectMapper;
    
    public InventoryEventListener(FeatureEngineer featureEngineer, ObjectMapper objectMapper) {
        this.featureEngineer = featureEngineer;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "inventory.events", groupId = "recommendation-service")
    public void handleInventoryEvent(String message) {
        try {
            InventoryEventDTO event = objectMapper.readValue(message, InventoryEventDTO.class);
            log.info("Received inventory event: productId={}, stock={}, eventType={}", 
                event.getProductId(), event.getStock(), event.getEventType());
            
            switch (event.getEventType()) {
                case "STOCK_UPDATED":
                    // Mettre à jour la disponibilité du produit
                    featureEngineer.updateProductStock(
                        event.getProductId(),
                        event.getStock(),
                        event.getReserved()
                    );
                    
                    if (event.getStock() == 0) {
                        log.warn("Product {} is out of stock - will be deprioritized", event.getProductId());
                    }
                    break;
                    
                case "LOW_STOCK":
                    log.info("Product {} has low stock ({}), applying urgency boost", 
                        event.getProductId(), event.getStock());
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing inventory event", e);
        }
    }
}
