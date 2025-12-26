package com.example.payment.dto;

public record PaymentResponse(
        String paymentId,
        String status,
        String message
) {}
