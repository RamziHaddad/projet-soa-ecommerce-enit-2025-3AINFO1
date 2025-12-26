package com.example.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        String paymentId,
        String userId,
        String cardNumber,
        BigDecimal amount
) {
    public UUID getPaymentIdAsUUID() {
        return UUID.fromString(paymentId);
    }

    public UUID getUserIdAsUUID() {
        return UUID.fromString(userId);
    }
}
