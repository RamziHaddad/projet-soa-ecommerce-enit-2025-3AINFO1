package com.onlineshop.order.mock.controller;

import com.onlineshop.order.dto.request.InventoryRequest;
import com.onlineshop.order.dto.request.InventoryItemRequest;
import com.onlineshop.order.dto.response.InventoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock controller pour le service Inventory
 * Simule les réponses du service de gestion du stock
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryMockController {
    
    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserveInventory(@RequestBody InventoryRequest request) {
        log.info("Mock Inventory Service - Reserving inventory for order: {}", request.getOrderNumber());
        
        // Simulation de réservation de stock (95% de succès)
        boolean reservationSuccess = Math.random() > 0.05;
        
        if (reservationSuccess) {
            String transactionId = "INV-" + UUID.randomUUID().toString().substring(0, 8);
            
            InventoryResponse response = InventoryResponse.builder()
                    .success(true)
                    .transactionId(transactionId)
                    .message("Stock réservé avec succès")
                    .build();
            
            log.info("Mock Inventory Service - Reservation successful. Transaction ID: {}", transactionId);
            return ResponseEntity.ok(response);
        } else {
            InventoryResponse response = InventoryResponse.builder()
                    .success(false)
                    .transactionId(null)
                    .message("Stock insuffisant pour certains articles")
                    .build();
            
            log.warn("Mock Inventory Service - Reservation failed for order: {}", request.getOrderNumber());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/release/{transactionId}")
    public ResponseEntity<InventoryResponse> releaseInventory(@PathVariable("transactionId") String transactionId) {
        log.info("Mock Inventory Service - Releasing inventory for transaction: {}", transactionId);
        
        // Simulation de libération de stock (toujours succès)
        InventoryResponse response = InventoryResponse.builder()
                .success(true)
                .transactionId(transactionId)
                .message("Stock libéré avec succès")
                .build();
        
        log.info("Mock Inventory Service - Stock released successfully for transaction: {}", transactionId);
        return ResponseEntity.ok(response);
    }
}
