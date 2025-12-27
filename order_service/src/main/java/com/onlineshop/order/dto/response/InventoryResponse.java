package com.onlineshop.order.dto.response;

import java.util.List;

public record InventoryResponse(
        boolean success,
        String orderId,
        String message,
        List<ItemReservation> items) {
    public static record ItemReservation(
            String reservationId,
            String productId,
            Integer quantity,
            String status) {
    }
}
