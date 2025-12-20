package com.example.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentRequest {

    public String paymentId;
    public String userId;
    public String cardNumber;
    public BigDecimal amount;

    public PaymentRequest() {}

    public PaymentRequest(String paymentId, String userId, String cardNumber, BigDecimal amount) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.cardNumber = cardNumber;
        this.amount = amount;
    }

    // Helper method to get UUID from string
    public UUID getPaymentIdAsUUID() {
        return UUID.fromString(paymentId);
    }

    public UUID getUserIdAsUUID() {
        return UUID.fromString(userId);
    }
}
