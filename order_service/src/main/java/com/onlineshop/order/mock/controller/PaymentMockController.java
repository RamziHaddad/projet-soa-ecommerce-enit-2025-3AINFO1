package com.onlineshop.order.mock.controller;

import com.onlineshop.order.dto.request.PaymentRequest;
import com.onlineshop.order.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Mock controller pour le service Payment
 * Simule les réponses du service de paiement
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentMockController {
    
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("Mock Payment Service - Processing payment for order: {}", request.getOrderNumber());
        
        // Simulation de traitement de paiement (95% de succès)
        boolean paymentSuccess = Math.random() > 0.05;
        
        if (paymentSuccess) {
            String transactionId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
            
            PaymentResponse response = PaymentResponse.builder()
                    .success(true)
                    .transactionId(transactionId)
                    .message("Paiement traité avec succès")
                    .build();
            
            log.info("Mock Payment Service - Payment successful. Transaction ID: {}", transactionId);
            return ResponseEntity.ok(response);
        } else {
            PaymentResponse response = PaymentResponse.builder()
                    .success(false)
                    .transactionId(null)
                    .message("Échec du paiement")
                    .build();
            
            log.warn("Mock Payment Service - Payment failed for order: {}", request.getOrderNumber());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/refund/{transactionId}")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable("transactionId") String transactionId) {
        log.info("Mock Payment Service - Refunding payment for transaction: {}", transactionId);
        
        // Simulation de remboursement (toujours succès)
        PaymentResponse response = PaymentResponse.builder()
                .success(true)
                .transactionId("REF-" + UUID.randomUUID().toString().substring(0, 8))
                .message("Remboursement effectué avec succès")
                .build();
        
        log.info("Mock Payment Service - Refund successful for transaction: {}", transactionId);
        return ResponseEntity.ok(response);
    }
}
