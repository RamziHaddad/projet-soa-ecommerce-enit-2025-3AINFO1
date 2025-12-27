package com.onlineshop.order.client;

import com.onlineshop.order.dto.request.ShippingRequest;
import com.onlineshop.order.dto.response.DeliveryResponse;
import com.onlineshop.order.dto.response.ShippingResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for Shipping Service
 */
@FeignClient(name = "shipping-service", url = "${services.shipping.url}")
public interface ShippingServiceClient {
    
    @PostMapping("/api/shipping/arrange")
    DeliveryResponse arrangeShipping(@RequestBody ShippingRequest request);
    
    @DeleteMapping("/api/deliveries/{trackingNumber}")
    ShippingResponse cancelShipping(@PathVariable("id") String trackingNumber);
}
