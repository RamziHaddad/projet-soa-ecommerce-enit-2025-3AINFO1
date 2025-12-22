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
                    .message("Inventory reserved")
                    .items(response.getItems().stream()
                            .map(ir -> InventoryReservationEvent.Item.builder()
                                    .reservationId(ir.getReservationId())
                                    .productId(ir.getProductId())
                                    .quantity(ir.getQuantity())
                                    .status(ReservationStatus.RESERVED)
                                    .build())
                            .toList())
                    .build();
            producer.publishInventoryEvent(event);
            // Optional one-shot notification summarizing reservation
            producer.publishNotification(NotificationEvent.builder()
                    .type(NotificationEvent.Type.RESERVED)
                    .orderId(event.getOrderId())
                    .message("Reservation created for " + event.getItems().size() + " item(s)")
                    .build());
        } catch (Exception ex) {
            log.error("Reservation failed for orderId={} reason={}", command.getOrderId(), ex.getMessage());
            InventoryReservationEvent event = InventoryReservationEvent.builder()
                    .success(false)
                    .orderId(command.getOrderId())
                    .message("Reservation failed: " + ex.getMessage())
                    .items(items.stream().map(i -> InventoryReservationEvent.Item.builder()
                            .reservationId(null)
                            .productId(i.getProductId())
                            .quantity(i.getQuantity())
                            .status(null)
                            .build()).toList())
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
        var cancelledEventItems = inventoryService.getReservationsByOrderId(command.getOrderId()).stream()
                .map(r -> InventoryReservationEvent.Item.builder()
                        .reservationId(r.getReservationId())
                        .productId(r.getProductId())
                        .quantity(r.getQuantity())
                        .status(ReservationStatus.CANCELLED)
                        .build())
                .toList();
                producer.publishInventoryEvent(InventoryReservationEvent.builder()
                .success(true)
                .orderId(command.getOrderId())
                .message("Reservation cancelled")
                .items(cancelledEventItems)
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
        // Reconstruct current reservations for the order to include reservationIds
        var items = inventoryService.getReservationsByOrderId(command.getOrderId()).stream()
                .map(r -> InventoryReservationEvent.Item.builder()
                        .reservationId(r.getReservationId())
                        .productId(r.getProductId())
                        .quantity(r.getQuantity())
                        .status(ReservationStatus.CONFIRMED)
                        .build())
                .toList();
        producer.publishInventoryEvent(InventoryReservationEvent.builder()
                .success(true)
                .orderId(command.getOrderId())
                .message("Reservation confirmed")
                .items(items)
                .build());
    }

}
