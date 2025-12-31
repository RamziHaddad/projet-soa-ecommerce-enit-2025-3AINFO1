package com.recommendation.integration.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recommendation.integration.dto.OrderEventDTO;
import com.recommendation.ml.FeatureEngineer;
import com.recommendation.ml.TrendingDetectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Listener pour les événements du Order Service
 * Topics: order.events
 */
@Slf4j
@Service
public class OrderEventListener {
    
    private final FeatureEngineer featureEngineer;
    private final TrendingDetectorService trendingDetectorService;
    private final ObjectMapper objectMapper;
    
    public OrderEventListener(FeatureEngineer featureEngineer, 
                             TrendingDetectorService trendingDetectorService,
                             ObjectMapper objectMapper) {
        this.featureEngineer = featureEngineer;
        this.trendingDetectorService = trendingDetectorService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "order.events", groupId = "recommendation-service")
    public void handleOrderEvent(String message) {
        try {
            OrderEventDTO event = objectMapper.readValue(message, OrderEventDTO.class);
            log.info("Received order event: orderId={}, customerId={}, status={}", 
                event.getOrderId(), event.getCustomerId(), event.getStatus());
            
            if ("CONFIRMED".equalsIgnoreCase(event.getStatus()) && event.getItems() != null) {
                // 1. Enregistrer l'achat pour les co-occurrences
                for (OrderEventDTO.OrderItemDTO item : event.getItems()) {
                    featureEngineer.recordPurchase(
                        String.valueOf(event.getCustomerId()),
                        item.getProductId(),
                        item.getQuantity()
                    );
                    
                    // 2. Mettre à jour les tendances
                    trendingDetectorService.recordEvent(
                        item.getProductId(),
                        "purchase",
                        item.getQuantity()
                    );
                }
                log.info("Order {} confirmed - {} items processed", 
                    event.getOrderNumber(), event.getItems().size());
            }
        } catch (Exception e) {
            log.error("Error processing order event", e);
        }
    }
}
