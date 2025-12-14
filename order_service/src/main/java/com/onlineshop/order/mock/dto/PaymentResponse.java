package com.onlineshop.order.mock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse du service Payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private boolean success;
    private String message;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private LocalDateTime transactionTime;
    private String status;
    
    public enum PaymentStatus {
        PENDING, AUTHORIZED, CAPTURED, FAILED, REFUNDED
    }
}
