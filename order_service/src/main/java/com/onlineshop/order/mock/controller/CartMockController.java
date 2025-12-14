package com.onlineshop.order.mock.controller;

import com.onlineshop.order.mock.dto.CartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mock controller pour le service Cart
 * Simule les réponses du service panier
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartMockController {
    
    @GetMapping("/{customerId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long customerId) {
        log.info("Mock Cart Service - Getting cart for customer: {}", customerId);
        
        // Simulation de données de panier
        CartResponse cartResponse = CartResponse.builder()
                .cartId(1000L + customerId)
                .customerId(customerId)
                .items(List.of(
                        CartResponse.CartItem.builder()
                                .productId(1L)
                                .productName("Produit A")
                                .quantity(2)
                                .unitPrice(BigDecimal.valueOf(29.99))
                                .totalPrice(BigDecimal.valueOf(59.98))
                                .build(),
                        CartResponse.CartItem.builder()
                                .productId(2L)
                                .productName("Produit B")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(49.99))
                                .totalPrice(BigDecimal.valueOf(49.99))
                                .build()
                ))
                .totalAmount(BigDecimal.valueOf(109.97))
                .valid(true)
                .message("Panier valide")
                .build();
        
        return ResponseEntity.ok(cartResponse);
    }
    
    @PostMapping("/validate")
    public ResponseEntity<CartResponse> validateCart(@RequestBody CartRequest request) {
        log.info("Mock Cart Service - Validating cart for customer: {}", request.getCustomerId());
        
        // Simulation d'une validation de panier
        boolean isValid = request.getCustomerId() != null && 
                         request.getItems() != null && 
                         !request.getItems().isEmpty();
        
        CartResponse response = CartResponse.builder()
                .cartId(1000L + request.getCustomerId())
                .customerId(request.getCustomerId())
                .items(request.getItems().stream()
                        .map(item -> CartResponse.CartItem.builder()
                                .productId(item.getProductId())
                                .productName("Produit " + item.getProductId())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                .build())
                        .toList())
                .totalAmount(request.getItems().stream()
                        .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .valid(isValid)
                .message(isValid ? "Panier valide" : "Panier invalide")
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    // Classe interne pour la requête
    public static class CartRequest {
        private Long customerId;
        private List<CartItem> items;
        
        // Getters et setters
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        
        public List<CartItem> getItems() { return items; }
        public void setItems(List<CartItem> items) { this.items = items; }
        
        public static class CartItem {
            private Long productId;
            private Integer quantity;
            private BigDecimal unitPrice;
            
            // Getters et setters
            public Long getProductId() { return productId; }
            public void setProductId(Long productId) { this.productId = productId; }
            
            public Integer getQuantity() { return quantity; }
            public void setQuantity(Integer quantity) { this.quantity = quantity; }
            
            public BigDecimal getUnitPrice() { return unitPrice; }
            public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        }
    }
}
