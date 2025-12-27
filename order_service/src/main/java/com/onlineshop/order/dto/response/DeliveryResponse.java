package com.onlineshop.order.dto.response;

import java.time.LocalDateTime;

public record DeliveryResponse(
    Long deliveryId,
    Long orderId,
	Long customerId, 
    String status,
    String trackingNumber,
    LocalDateTime createdAt) {

}
