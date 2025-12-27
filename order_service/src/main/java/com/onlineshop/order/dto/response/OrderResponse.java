package com.onlineshop.order.dto.response;

import com.onlineshop.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String shippingAddress,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
