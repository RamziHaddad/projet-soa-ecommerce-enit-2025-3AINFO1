package com.onlineshop.order.dto.response;

import java.time.LocalDateTime;

public record ShippingResponse(
        Boolean success,
        String trackingNumber,
        String message,
        Boolean retryable,
        LocalDateTime timestamp) {
}
