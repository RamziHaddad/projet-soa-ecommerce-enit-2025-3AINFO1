package com.onlineshop.order.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;

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
    
    @Builder.Default
    @Column(name = "inventory_reserved")
    private Boolean inventoryReserved = false;
    
    @Builder.Default
    @Column(name = "payment_processed")
    private Boolean paymentProcessed = false;
    
    @Builder.Default
    @Column(name = "shipping_arranged")
    private Boolean shippingArranged = false;
    
    @Column(name = "inventory_transaction_id")
    private String inventoryTransactionId;
    
    @Column(name = "payment_transaction_id")
    private String paymentTransactionId;
    
    @Column(name = "shipping_transaction_id")
    private String shippingTransactionId;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Builder.Default
    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column
    private LocalDateTime lastRetryTime;

    @Column
    private LocalDateTime nextRetryTime;

    @Column
    private Integer maxRetries;

    @Column
    private Boolean retryable;

    @Column(columnDefinition = "TEXT")
    private String lastErrorStackTrace;

    @Column
    private LocalDateTime recoveryStartedAt;

    @Column
    private LocalDateTime recoveryCompletedAt;

    @Column(columnDefinition = "TEXT")
    private String recoveryNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }
}
