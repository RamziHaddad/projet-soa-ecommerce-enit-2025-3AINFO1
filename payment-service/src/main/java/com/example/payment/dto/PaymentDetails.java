package com.example.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentDetails {
    public UUID paymentId;
    public UUID userId;
    public String cardNumber;
    public BigDecimal amount;
    public String status;
    public int attempts;
    public String previousStep;
    public String nextStep;
    public LocalDateTime createdAt;

    public PaymentDetails() {}

    public PaymentDetails(UUID paymentId, UUID userId, String cardNumber, BigDecimal amount,
                         String status, int attempts, String previousStep, String nextStep,
                         LocalDateTime createdAt) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.cardNumber = cardNumber;
        this.amount = amount;
        this.status = status;
        this.attempts = attempts;
        this.previousStep = previousStep;
        this.nextStep = nextStep;
        this.createdAt = createdAt;
    }
}
