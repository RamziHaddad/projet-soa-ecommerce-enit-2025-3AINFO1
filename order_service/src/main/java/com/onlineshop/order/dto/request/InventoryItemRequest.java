package com.onlineshop.order.dto.request;


public record InventoryItemRequest(
        String productId,
        Integer quantity) {
}

