package com.onlineshop.order.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "saga_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStep currentStep;
    
    @Column(name = "inventory_reserved")
    private Boolean inventoryReserved;
    
    @Column(name = "payment_processed")
    private Boolean paymentProcessed;
    
    @Column(name = "shipping_arranged")
    private Boolean shippingArranged;
    
    @Column(name = "inventory_transaction_id")
    private String inventoryTransactionId;
    
    @Column(name = "payment_transaction_id")
    private String paymentTransactionId;
    
    @Column(name = "shipping_transaction_id")
    private String shippingTransactionId;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(nullable = false)
    private Integer retryCount;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (retryCount == null) {
            retryCount = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
