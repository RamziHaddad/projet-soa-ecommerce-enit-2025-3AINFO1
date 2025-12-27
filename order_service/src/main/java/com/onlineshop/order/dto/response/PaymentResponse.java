package com.onlineshop.order.dto.response;

import java.time.LocalDateTime;

public record PaymentResponse(
        Boolean success,
        String transactionId,
        String message,
        Boolean retryable,
        LocalDateTime timestamp) {
}
