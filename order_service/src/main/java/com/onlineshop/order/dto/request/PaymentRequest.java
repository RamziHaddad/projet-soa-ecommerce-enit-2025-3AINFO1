package com.onlineshop.order.dto.request;

import java.math.BigDecimal;

public record PaymentRequest(
        String orderNumber,
        Long customerId,
        BigDecimal amount,
        String paymentMethod) {
}
