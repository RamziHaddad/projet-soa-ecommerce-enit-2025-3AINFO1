package com.onlineshop.order.mock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de réponse du service Shipping
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingResponse {
    
    private boolean success;
    private String message;
    private String shippingId;
    private String trackingNumber;
    private String carrier;
    private String serviceType;
    private LocalDate estimatedDeliveryDate;
    private LocalDateTime createdAt;
    private String status;
    private String shippingAddress;
    
    public enum ShippingStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }
}
