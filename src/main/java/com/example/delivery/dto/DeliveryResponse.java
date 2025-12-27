package com.example.delivery.dto;

import java.time.LocalDateTime;

public class DeliveryResponse {
    private Long deliveryId;
    private Long orderId;
	private Long customerId; 
    private String status;
    private String trackingNumber;
    private LocalDateTime createdAt;

    // getters & setters

    public Long getDeliveryId() { return deliveryId; }
    public void setDeliveryId(Long deliveryId) { this.deliveryId = deliveryId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
 
	public Long getCustomerId() { return customerId; }  // <-- getter
    public void setCustomerId(Long customerId) { this.customerId = customerId; } 

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
