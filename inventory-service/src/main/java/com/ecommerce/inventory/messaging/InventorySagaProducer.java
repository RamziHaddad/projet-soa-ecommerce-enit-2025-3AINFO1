package com.ecommerce.inventory.messaging;

import com.ecommerce.inventory.messaging.events.InventoryReservationEvent;
import com.ecommerce.inventory.messaging.events.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventorySagaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${saga.topics.inventoryEvents:inventory.events}")
    private String inventoryEventsTopic;

    @Value("${saga.topics.commNotifications:comm.notifications}")
    private String commNotificationsTopic;

    public void publishInventoryEvent(InventoryReservationEvent event) {
        log.info("Publishing inventory event to {}: {}", inventoryEventsTopic, event);
        kafkaTemplate.send(inventoryEventsTopic, event.getOrderId(), event);
    }

    public void publishNotification(NotificationEvent event) {
        log.info("Publishing notification event to {}: {}", commNotificationsTopic, event);
        kafkaTemplate.send(commNotificationsTopic, event.getOrderId(), event);
    }
}
