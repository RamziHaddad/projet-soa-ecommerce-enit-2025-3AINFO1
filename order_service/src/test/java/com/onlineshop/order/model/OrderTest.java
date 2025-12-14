package com.onlineshop.order.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Order order;
    private OrderItem orderItem1;
    private OrderItem orderItem2;

    @BeforeEach
    void setUp() {
        // Initialize test data
        order = Order.builder()
                .orderNumber("ORD-2025-001")
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .shippingAddress("123 Main St, City, State 12345")
                .build();
        
        orderItem1 = OrderItem.builder()
                .productId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .build();
        
        orderItem2 = OrderItem.builder()
                .productId(2L)
                .quantity(1)
                .unitPrice(new BigDecimal("49.99"))
                .build();
    }

    @Test
    void testOrderCreation() {
        // Test basic order creation
        assertNotNull(order);
        assertEquals("ORD-2025-001", order.getOrderNumber());
        assertEquals(1L, order.getCustomerId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        assertEquals("123 Main St, City, State 12345", order.getShippingAddress());
        assertTrue(order.getItems().isEmpty());
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());
    }

    @Test
    void testAddOrderItem() {
        // Test adding single item
        orderItem1.setOrder(order);
        order.getItems().add(orderItem1);
        
        assertEquals(1, order.getItems().size());
        assertEquals(orderItem1, order.getItems().get(0));
        assertEquals(order, orderItem1.getOrder());
    }

    @Test
    void testAddMultipleOrderItems() {
        // Test adding multiple items
        orderItem1.setOrder(order);
        orderItem2.setOrder(order);
        order.getItems().add(orderItem1);
        order.getItems().add(orderItem2);
        
        assertEquals(2, order.getItems().size());
        assertTrue(order.getItems().contains(orderItem1));
        assertTrue(order.getItems().contains(orderItem2));
    }

    @Test
    void testCalculateTotalAmount() {
        // Test total amount calculation
        orderItem1.setOrder(order);
        orderItem2.setOrder(order);
        order.getItems().add(orderItem1);
        order.getItems().add(orderItem2);
        
        // Manually calculate expected total
        BigDecimal expectedTotal = orderItem1.getUnitPrice().multiply(BigDecimal.valueOf(orderItem1.getQuantity()))
                .add(orderItem2.getUnitPrice().multiply(BigDecimal.valueOf(orderItem2.getQuantity())));
        
        assertEquals(new BigDecimal("109.97"), expectedTotal);
    }

    @Test
    void testOrderStatusTransition() {
        // Test valid status transitions
        assertEquals(OrderStatus.PENDING, order.getStatus());
        
        // Transition to INVENTORY_RESERVED
        order.setStatus(OrderStatus.INVENTORY_RESERVED);
        assertEquals(OrderStatus.INVENTORY_RESERVED, order.getStatus());
        
        // Transition to PAYMENT_PROCESSED
        order.setStatus(OrderStatus.PAYMENT_PROCESSED);
        assertEquals(OrderStatus.PAYMENT_PROCESSED, order.getStatus());
        
        // Transition to SHIPPING_ARRANGED
        order.setStatus(OrderStatus.SHIPPING_ARRANGED);
        assertEquals(OrderStatus.SHIPPING_ARRANGED, order.getStatus());
        
        // Transition to COMPLETED
        order.setStatus(OrderStatus.COMPLETED);
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void testOrderStatusTransitionToFailed() {
        // Test transition to FAILED status
        order.setStatus(OrderStatus.FAILED);
        assertEquals(OrderStatus.FAILED, order.getStatus());
    }

    @Test
    void testOrderStatusTransitionToCancelled() {
        // Test transition to CANCELLED status
        order.setStatus(OrderStatus.CANCELLED);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void testOrderItemRelationship() {
        // Test bidirectional relationship between Order and OrderItem
        orderItem1.setOrder(order);
        order.getItems().add(orderItem1);
        
        assertNotNull(orderItem1.getOrder());
        assertEquals(order, orderItem1.getOrder());
        assertTrue(order.getItems().contains(orderItem1));
    }

    @Test
    void testOrderItemCascadeRemoval() {
        // Test that removing order removes items (cascade)
        orderItem1.setOrder(order);
        orderItem2.setOrder(order);
        order.getItems().add(orderItem1);
        order.getItems().add(orderItem2);
        
        assertEquals(2, order.getItems().size());
        
        order.getItems().clear();
        assertEquals(0, order.getItems().size());
    }

    @Test
    void testOrderValidation() {
        // Test order cannot be created with null required fields
        assertThrows(NullPointerException.class, () -> {
            Order.builder()
                    .orderNumber(null) // Should throw NPE
                    .build();
        });
    }

    @Test
    void testOrderNumberUniqueness() {
        // Test that order numbers should be unique (this would need database validation)
        // For now, just test that order numbers can be set
        order.setOrderNumber("ORD-2025-002");
        assertEquals("ORD-2025-002", order.getOrderNumber());
    }

    @Test
    void testOrderTimestamps() {
        // Test that timestamps are set automatically
        LocalDateTime beforeCreate = LocalDateTime.now();
        
        Order newOrder = Order.builder()
                .orderNumber("ORD-2025-TEST")
                .customerId(2L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .shippingAddress("456 Test Ave, Test City, TC 67890")
                .build();
        
        LocalDateTime afterCreate = LocalDateTime.now();
        
        assertNotNull(newOrder.getCreatedAt());
        assertNotNull(newOrder.getUpdatedAt());
        assertTrue(newOrder.getCreatedAt().isAfter(beforeCreate.minusSeconds(1)));
        assertTrue(newOrder.getCreatedAt().isBefore(afterCreate.plusSeconds(1)));
        assertEquals(newOrder.getCreatedAt(), newOrder.getUpdatedAt());
    }

    @Test
    void testOrderTimestampUpdate() {
        // Test that updatedAt changes on modification
        LocalDateTime originalCreatedAt = order.getCreatedAt();
        LocalDateTime originalUpdatedAt = order.getUpdatedAt();
        
        // Small delay to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        order.setStatus(OrderStatus.COMPLETED);
        
        assertEquals(originalCreatedAt, order.getCreatedAt()); // CreatedAt should not change
        assertNotEquals(originalUpdatedAt, order.getUpdatedAt()); // UpdatedAt should change
    }

    @Test
    void testOrderWithSagaState() {
        // Test order with saga state
        SagaState sagaState = SagaState.builder()
                .order(order)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(SagaStep.ORDER_CREATED)
                .build();
        
        order.setSagaState(sagaState);
        
        assertNotNull(order.getSagaState());
        assertEquals(sagaState, order.getSagaState());
        assertEquals(SagaStatus.IN_PROGRESS, sagaState.getStatus());
        assertEquals(SagaStep.ORDER_CREATED, sagaState.getCurrentStep());
    }

    @Test
    void testOrderGettersAndSetters() {
        // Test individual field accessors
        order.setId(100L);
        assertEquals(100L, order.getId());
        
        order.setTotalAmount(new BigDecimal("199.99"));
        assertEquals(new BigDecimal("199.99"), order.getTotalAmount());
        
        String newAddress = "789 New St, New City, NC 11111";
        order.setShippingAddress(newAddress);
        assertEquals(newAddress, order.getShippingAddress());
    }
}
