package com.ecommerce.inventory.messaging;

import com.ecommerce.inventory.dto.ReserveInventoryRequest;
import com.ecommerce.inventory.dto.ReserveInventoryResponse;
import com.ecommerce.inventory.client.OrdersClient;
import com.ecommerce.inventory.messaging.commands.InventoryCancelCommand;
import com.ecommerce.inventory.messaging.commands.InventoryConfirmCommand;
import com.ecommerce.inventory.messaging.commands.InventoryReserveCommand;
import com.ecommerce.inventory.messaging.events.InventoryReservationEvent;
import com.ecommerce.inventory.messaging.events.NotificationEvent;
import com.ecommerce.inventory.model.ReservationStatus;
import com.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventorySagaListener {

    private final InventoryService inventoryService;
    private final InventorySagaProducer producer;
        private final OrdersClient ordersClient;

    @Value("${saga.topics.inventoryReserveCommand:inventory.reserve.command}")
    private String reserveTopic;

    @Value("${saga.topics.inventoryCancelCommand:inventory.cancel.command}")
    private String cancelTopic;

    @Value("${saga.topics.inventoryConfirmCommand:inventory.confirm.command}")
    private String confirmTopic;

    @KafkaListener(topics = "#{'${saga.topics.inventoryReserveCommand:inventory.reserve.command}'}",
            groupId = "#{'${spring.kafka.consumer.group-id}'}")
    public void onReserveCommand(@Payload InventoryReserveCommand command) {
        log.info("Received InventoryReserveCommand: {}", command);

        // Map command to service request; if items missing, fetch from Orders Service
        List<ReserveInventoryRequest.ItemRequest> items = (command.getItems() != null && !command.getItems().isEmpty()
                ? command.getItems()
                : ordersClient.getOrderItems(command.getOrderId()).stream()
                    .map(m -> new InventoryReserveCommand.Item((String) m.get("productId"), (Integer) m.get("quantity")))
                    .toList())
                .stream()
                .map(i -> ReserveInventoryRequest.ItemRequest.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .build())
                .toList();

        ReserveInventoryResponse response;
        try {
            response = inventoryService.reserveInventory(ReserveInventoryRequest.builder()
                    .orderId(command.getOrderId())
                    .items(items)
                    .build());

            InventoryReservationEvent event = InventoryReservationEvent.builder()
                    .success(true)
                    .orderId(response.getOrderId())
                    .reservationId(response.getReservationId())
                    .productId(items.get(0).getProductId())
                    .quantity(items.get(0).getQuantity())
                    .status(ReservationStatus.RESERVED)
                    .message("Inventory reserved")
                    .build();
            producer.publishInventoryEvent(event);
            producer.publishNotification(NotificationEvent.builder()
                    .type(NotificationEvent.Type.RESERVED)
                    .orderId(event.getOrderId())
                    .productId(event.getProductId())
                    .quantity(event.getQuantity())
                    .message("Reservation created")
                    .build());
        } catch (Exception ex) {
            log.error("Reservation failed for orderId={} reason={}", command.getOrderId(), ex.getMessage());
            InventoryReservationEvent event = InventoryReservationEvent.builder()
                    .success(false)
                    .orderId(command.getOrderId())
                    .reservationId(null)
                    .productId(items.get(0).getProductId())
                    .quantity(items.get(0).getQuantity())
                    .status(null)
                    .message("Reservation failed: " + ex.getMessage())
                    .build();
            producer.publishInventoryEvent(event);
        }
    }

    @KafkaListener(topics = "#{'${saga.topics.inventoryCancelCommand:inventory.cancel.command}'}",
            groupId = "#{'${spring.kafka.consumer.group-id}'}")
    public void onCancelCommand(@Payload InventoryCancelCommand command) {
        log.info("Received InventoryCancelCommand: {}", command);
        inventoryService.cancelReservation(command.getOrderId());
        producer.publishNotification(NotificationEvent.builder()
                .type(NotificationEvent.Type.CANCELLED)
                .orderId(command.getOrderId())
                .message("Reservation cancelled")
                .build());
        // Emit inventory event to orchestrator
        producer.publishInventoryEvent(InventoryReservationEvent.builder()
                .success(true)
                .orderId(command.getOrderId())
                .message("Reservation cancelled")
                .status(ReservationStatus.CANCELLED)
                .build());
    }

    @KafkaListener(topics = "#{'${saga.topics.inventoryConfirmCommand:inventory.confirm.command}'}",
            groupId = "#{'${spring.kafka.consumer.group-id}'}")
    public void onConfirmCommand(@Payload InventoryConfirmCommand command) {
        log.info("Received InventoryConfirmCommand: {}", command);
        inventoryService.confirmReservation(command.getOrderId());
        producer.publishNotification(NotificationEvent.builder()
                .type(NotificationEvent.Type.CONFIRMED)
                .orderId(command.getOrderId())
                .message("Reservation confirmed")
                .build());
        // Emit inventory event to orchestrator
        producer.publishInventoryEvent(InventoryReservationEvent.builder()
                .success(true)
                .orderId(command.getOrderId())
                .message("Reservation confirmed")
                .status(ReservationStatus.CONFIRMED)
                .build());
    }
}
