package com.example.delivery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DeliveryRequest {

    @NotNull
    private Long orderId;
    private Long customerId; 

    @NotNull
    @Size(min = 5)
    private String address;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getCustomerId() { return customerId; } // <-- getter
    public void setCustomerId(Long customerId) { this.customerId = customerId; } // <-- setter

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
