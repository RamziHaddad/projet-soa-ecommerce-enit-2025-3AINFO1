package com.ecommerce.inventory.dto;

import com.ecommerce.inventory.model.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveInventoryResponse {

    private boolean success;
    private String orderId;
    private String message;
    private List<ItemReservation> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemReservation {
        private UUID reservationId;
        private String productId;
        private Integer quantity;
        private ReservationStatus status;
    }
}
