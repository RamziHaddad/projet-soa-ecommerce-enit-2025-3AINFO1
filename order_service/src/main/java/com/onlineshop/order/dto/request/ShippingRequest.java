package com.onlineshop.order.dto.request;

public record ShippingRequest(
        String orderNumber,
        Long customerId,
        String shippingAddress) {
}
