package com.example.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentDetails(
        UUID paymentId,
        UUID userId,
        String cardNumber,
        BigDecimal amount,
        String status,
        int attempts,
        String previousStep,
        String nextStep,
        LocalDateTime createdAt
) {}
