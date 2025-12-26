package com.example.payment.dto;

import java.math.BigDecimal;

public record OrderPaymentRequest(
        String orderNumber,
        Long customerId,
        BigDecimal amount,
        String paymentMethod
) {}
