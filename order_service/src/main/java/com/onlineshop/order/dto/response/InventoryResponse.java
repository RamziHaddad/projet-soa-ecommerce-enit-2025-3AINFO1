package com.onlineshop.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private boolean success;
    private String orderId;
    private String message;
    private java.util.List<ItemReservation> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemReservation {
        private String reservationId;
        private String productId;
        private Integer quantity;
        private String status;
    }
}
