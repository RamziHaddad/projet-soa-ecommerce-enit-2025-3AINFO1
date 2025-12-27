package com.onlineshop.order.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SagaStateTest {

    private SagaState sagaState;
    private Order order;

    @BeforeEach
    void setUp() {
        // Initialize test data
        order = Order.builder()
                .orderNumber("ORD-2025-001")
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(java.math.BigDecimal.ZERO)
                .shippingAddress("123 Main St, City, State 12345")
                .build();
        
        sagaState = SagaState.builder()
                .order(order)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(SagaStep.ORDER_CREATED)
                .build();
        order.setSagaState(sagaState);
        sagaState.onCreate();
    }

    @Test
    void testSagaStateCreation() {
        // Test basic saga state creation
        assertNotNull(sagaState);
        assertEquals(SagaStatus.IN_PROGRESS, sagaState.getStatus());
        assertEquals(SagaStep.ORDER_CREATED, sagaState.getCurrentStep());
        assertEquals(order, sagaState.getOrder());
        assertFalse(sagaState.getInventoryReserved());
        assertFalse(sagaState.getPaymentProcessed());
        assertFalse(sagaState.getShippingArranged());
        assertEquals(0, sagaState.getRetryCount());
        assertNotNull(sagaState.getCreatedAt());
        assertNotNull(sagaState.getUpdatedAt());
    }

    @Test
    void testSagaStatusTransition() {
        // Test valid status transitions
        assertEquals(SagaStatus.IN_PROGRESS, sagaState.getStatus());
        
        // Transition to COMPLETED
        sagaState.setStatus(SagaStatus.COMPLETED);
        assertEquals(SagaStatus.COMPLETED, sagaState.getStatus());
        
        // Transition to FAILED
        sagaState.setStatus(SagaStatus.FAILED);
        assertEquals(SagaStatus.FAILED, sagaState.getStatus());
        
        // Transition to COMPENSATING
        sagaState.setStatus(SagaStatus.COMPENSATING);
        assertEquals(SagaStatus.COMPENSATING, sagaState.getStatus());
    }

    @Test
    void testCurrentStepUpdate() {
        // Test step progression
        assertEquals(SagaStep.ORDER_CREATED, sagaState.getCurrentStep());
        
        // Move to inventory validation
        sagaState.setCurrentStep(SagaStep.INVENTORY_VALIDATION);
        assertEquals(SagaStep.INVENTORY_VALIDATION, sagaState.getCurrentStep());
        
        // Move to payment processing
        sagaState.setCurrentStep(SagaStep.PAYMENT_PROCESSING);
        assertEquals(SagaStep.PAYMENT_PROCESSING, sagaState.getCurrentStep());
        
        // Move to shipping arrangement
        sagaState.setCurrentStep(SagaStep.SHIPPING_ARRANGEMENT);
        assertEquals(SagaStep.SHIPPING_ARRANGEMENT, sagaState.getCurrentStep());
        
        // Move to order confirmation
        sagaState.setCurrentStep(SagaStep.ORDER_CONFIRMATION);
        assertEquals(SagaStep.ORDER_CONFIRMATION, sagaState.getCurrentStep());
        
        // Move to completed
        sagaState.setCurrentStep(SagaStep.COMPLETED);
        assertEquals(SagaStep.COMPLETED, sagaState.getCurrentStep());
    }

    @Test
    void testInventoryReservationTracking() {
        // Test inventory reservation flag
        assertFalse(sagaState.getInventoryReserved());
        
        sagaState.setInventoryReserved(true);
        assertTrue(sagaState.getInventoryReserved());
        
        sagaState.setInventoryReserved(false);
        assertFalse(sagaState.getInventoryReserved());
    }

    @Test
    void testPaymentProcessingTracking() {
        // Test payment processing flag
        assertFalse(sagaState.getPaymentProcessed());
        
        sagaState.setPaymentProcessed(true);
        assertTrue(sagaState.getPaymentProcessed());
        
        sagaState.setPaymentProcessed(false);
        assertFalse(sagaState.getPaymentProcessed());
    }

    @Test
    void testShippingArrangementTracking() {
        // Test shipping arrangement flag
        assertFalse(sagaState.getShippingArranged());
        
        sagaState.setShippingArranged(true);
        assertTrue(sagaState.getShippingArranged());
        
        sagaState.setShippingArranged(false);
        assertFalse(sagaState.getShippingArranged());
    }

    @Test
    void testTransactionIdTracking() {
        // Test transaction ID storage
        String inventoryTxnId = "INV-TXN-123";
        sagaState.setInventoryTransactionId(inventoryTxnId);
        assertEquals(inventoryTxnId, sagaState.getInventoryTransactionId());
        
        String paymentTxnId = "PAY-TXN-456";
        sagaState.setPaymentTransactionId(paymentTxnId);
        assertEquals(paymentTxnId, sagaState.getPaymentTransactionId());
        
        String shippingTxnId = "SHIP-TXN-789";
        sagaState.setShippingTransactionId(shippingTxnId);
        assertEquals(shippingTxnId, sagaState.getShippingTransactionId());
    }

    @Test
    void testErrorMessageStorage() {
        // Test error message storage
        String errorMessage = "Inventory service unavailable";
        sagaState.setErrorMessage(errorMessage);
        assertEquals(errorMessage, sagaState.getErrorMessage());
        
        // Clear error message
        sagaState.setErrorMessage(null);
        assertNull(sagaState.getErrorMessage());
    }

    @Test
    void testRetryCountManagement() {
        // Test retry count increment
        assertEquals(0, sagaState.getRetryCount());
        
        sagaState.setRetryCount(1);
        assertEquals(1, sagaState.getRetryCount());
        
        sagaState.setRetryCount(5);
        assertEquals(5, sagaState.getRetryCount());
    }

    @Test
    void testSagaStateTimestamps() {
        // Test that timestamps are set automatically
        LocalDateTime beforeCreate = LocalDateTime.now();
        
        SagaState newSagaState = SagaState.builder()
                .order(order)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(SagaStep.ORDER_CREATED)
                .build();
        newSagaState.onCreate();
        
        LocalDateTime afterCreate = LocalDateTime.now();
        
        assertNotNull(newSagaState.getCreatedAt());
        assertNotNull(newSagaState.getUpdatedAt());
        assertTrue(newSagaState.getCreatedAt().isAfter(beforeCreate.minusSeconds(1)));
        assertTrue(newSagaState.getCreatedAt().isBefore(afterCreate.plusSeconds(1)));
        assertEquals(newSagaState.getCreatedAt(), newSagaState.getUpdatedAt());
        assertEquals(0, newSagaState.getRetryCount()); // Default retry count
    }


    @Test
    void testSagaStateWithOrder() {
        // Test bidirectional relationship with Order
        assertNotNull(sagaState.getOrder());
        assertEquals(order, sagaState.getOrder());
        assertEquals(sagaState, order.getSagaState());
    }

    @Test
    void testCompleteSagaWorkflow() {
        // Test a complete saga workflow simulation
        assertEquals(SagaStep.ORDER_CREATED, sagaState.getCurrentStep());
        assertFalse(sagaState.getInventoryReserved());
        assertFalse(sagaState.getPaymentProcessed());
        assertFalse(sagaState.getShippingArranged());
        
        // Simulate inventory reservation
        sagaState.setCurrentStep(SagaStep.INVENTORY_VALIDATION);
        sagaState.setInventoryReserved(true);
        sagaState.setInventoryTransactionId("INV-001");
        
        assertEquals(SagaStep.INVENTORY_VALIDATION, sagaState.getCurrentStep());
        assertTrue(sagaState.getInventoryReserved());
        assertEquals("INV-001", sagaState.getInventoryTransactionId());
        
        // Simulate payment processing
        sagaState.setCurrentStep(SagaStep.PAYMENT_PROCESSING);
        sagaState.setPaymentProcessed(true);
        sagaState.setPaymentTransactionId("PAY-001");
        
        assertEquals(SagaStep.PAYMENT_PROCESSING, sagaState.getCurrentStep());
        assertTrue(sagaState.getPaymentProcessed());
        assertEquals("PAY-001", sagaState.getPaymentTransactionId());
        
        // Simulate shipping arrangement
        sagaState.setCurrentStep(SagaStep.SHIPPING_ARRANGEMENT);
        sagaState.setShippingArranged(true);
        sagaState.setShippingTransactionId("SHIP-001");
        
        assertEquals(SagaStep.SHIPPING_ARRANGEMENT, sagaState.getCurrentStep());
        assertTrue(sagaState.getShippingArranged());
        assertEquals("SHIP-001", sagaState.getShippingTransactionId());
        
        // Complete the saga
        sagaState.setCurrentStep(SagaStep.COMPLETED);
        sagaState.setStatus(SagaStatus.COMPLETED);
        
        assertEquals(SagaStep.COMPLETED, sagaState.getCurrentStep());
        assertEquals(SagaStatus.COMPLETED, sagaState.getStatus());
    }

    @Test
    void testSagaStateGettersAndSetters() {
        // Test individual field accessors
        sagaState.setId(100L);
        assertEquals(100L, sagaState.getId());
        
        sagaState.setRetryCount(3);
        assertEquals(3, sagaState.getRetryCount());
        
        sagaState.setStatus(SagaStatus.FAILED);
        assertEquals(SagaStatus.FAILED, sagaState.getStatus());
        
        sagaState.setCurrentStep(SagaStep.PAYMENT_PROCESSING);
        assertEquals(SagaStep.PAYMENT_PROCESSING, sagaState.getCurrentStep());
    }

    @Test
    void testDefaultRetryCount() {
        // Test that retry count defaults to 0
        SagaState newSagaState = SagaState.builder()
                .order(order)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(SagaStep.ORDER_CREATED)
                // Not setting retryCount explicitly
                .build();
        
        assertEquals(0, newSagaState.getRetryCount());
    }
}
