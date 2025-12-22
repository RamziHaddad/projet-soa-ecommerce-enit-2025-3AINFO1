package com.onlineshop.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingResponse {

    private Boolean success;
    private String trackingNumber;
    private String message;
    private Boolean retryable;
    private LocalDateTime timestamp;
}
