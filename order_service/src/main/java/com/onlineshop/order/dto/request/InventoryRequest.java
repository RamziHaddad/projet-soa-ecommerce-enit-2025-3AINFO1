package com.onlineshop.order.dto.request;

import java.util.List;

public record InventoryRequest(
        String orderId,
        List<InventoryItemRequest> items
) {
} 
