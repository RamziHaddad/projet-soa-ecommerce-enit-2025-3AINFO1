package com.onlineshop.order.client;

import com.onlineshop.order.dto.request.InventoryRequest;
import com.onlineshop.order.dto.response.InventoryResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for Inventory Service
 */
@FeignClient(name = "inventory-service", url = "${services.inventory.url}")
public interface InventoryServiceClient {

    @PostMapping("/inventory/reservations")
    InventoryResponse reserveInventory(@RequestBody InventoryRequest request);

    @PostMapping("/inventory/reservations/{orderId}/cancel")
    void cancelReservation(@PathVariable("orderId") String orderId);

    @PostMapping("/inventory/reservations/{orderId}/confirm")
    void confirmReservation(@PathVariable("orderId") String orderId);
}
