package com.ecommerce.inventory.messaging.events;

import com.ecommerce.inventory.model.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationEvent {
    private boolean success;
    private String orderId;
    private UUID reservationId;
    private String productId;
    private Integer quantity;
    private ReservationStatus status;
    private String message;
}
