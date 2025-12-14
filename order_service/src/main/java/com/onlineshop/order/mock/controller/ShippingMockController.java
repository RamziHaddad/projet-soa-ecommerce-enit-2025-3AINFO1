package com.onlineshop.order.mock.controller;

import com.onlineshop.order.dto.request.ShippingRequest;
import com.onlineshop.order.dto.response.ShippingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Mock controller pour le service Shipping
 * Simule les réponses du service de livraison
 */
@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
@Slf4j
public class ShippingMockController {
    
    @PostMapping("/arrange")
    public ResponseEntity<ShippingResponse> arrangeShipping(@RequestBody ShippingRequest request) {
        log.info("Mock Shipping Service - Arranging shipping for order: {}", request.getOrderNumber());
        
        // Simulation d'arrangement de livraison (95% de succès)
        boolean shippingSuccess = Math.random() > 0.05;
        
        if (shippingSuccess) {
            String trackingNumber = "SHIP-" + UUID.randomUUID().toString().substring(0, 8);
            
            ShippingResponse response = ShippingResponse.builder()
                    .success(true)
                    .trackingNumber(trackingNumber)
                    .message("Livraison arrange avec succès")
                    .build();
            
            log.info("Mock Shipping Service - Shipping arranged successfully. Tracking: {}", trackingNumber);
            return ResponseEntity.ok(response);
        } else {
            ShippingResponse response = ShippingResponse.builder()
                    .success(false)
                    .trackingNumber(null)
                    .message("Échec de l'arrangement de livraison")
                    .build();
            
            log.warn("Mock Shipping Service - Shipping arrangement failed for order: {}", request.getOrderNumber());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/cancel/{trackingNumber}")
    public ResponseEntity<ShippingResponse> cancelShipping(@PathVariable("trackingNumber") String trackingNumber) {
        log.info("Mock Shipping Service - Cancelling shipping for tracking: {}", trackingNumber);
        
        // Simulation d'annulation de livraison (toujours succès)
        ShippingResponse response = ShippingResponse.builder()
                .success(true)
                .trackingNumber(trackingNumber)
                .message("Livraison annulée avec succès")
                .build();
        
        log.info("Mock Shipping Service - Shipping cancelled successfully for tracking: {}", trackingNumber);
        return ResponseEntity.ok(response);
    }
}
