package com.onlineshop.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    private String orderNumber;
    private Long customerId;
    private BigDecimal amount;
    private String paymentMethod;
}
