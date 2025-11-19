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
    
    @PostMapping("/api/inventory/reserve")
    InventoryResponse reserveInventory(@RequestBody InventoryRequest request);
    
    @PostMapping("/api/inventory/release/{transactionId}")
    InventoryResponse releaseInventory(@PathVariable("transactionId") String transactionId);
}
