package com.onlineshop.order.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onlineshop.order.config.OrderServiceConfig;
import com.onlineshop.order.exception.OrderNotFoundException;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.OrderStatus;
import com.onlineshop.order.repository.OrderRepository;
import com.onlineshop.order.saga.SagaOrchestrator;

@ExtendWith(MockitoExtension.class)
class OrderCancellationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SagaOrchestrator sagaOrchestrator;

    @Mock
    private OrderServiceConfig orderServiceConfig;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .orderNumber("TEST-ORD-001")
                .customerId(123L)
                .status(OrderStatus.COMPLETED)
                .totalAmount(BigDecimal.valueOf(100.00))
                .shippingAddress("123 Test St")
                .createdAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now().minusHours(1)) // Completed 1 hour ago
                .items(new ArrayList<>())
                .build();
    }

    @Test
    void testCancelCompletedOrderWithinWindow() {
        // Setup: Order completed 1 hour ago, window is 24 hours
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(testOrder));
        when(orderServiceConfig.getCancellationWindowHours()).thenReturn(24);

        // Execute: Should not throw exception
        assertDoesNotThrow(() -> orderService.cancelOrder(1L));

        // Verify: Order status should be updated to CANCELLED
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void testCancelCompletedOrderOutsideWindow() {
        // Setup: Order completed 25 hours ago, window is 24 hours
        testOrder.setUpdatedAt(LocalDateTime.now().minusHours(25));
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(testOrder));
        when(orderServiceConfig.getCancellationWindowHours()).thenReturn(24);

        // Execute & Verify: Should throw IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(1L));

        assertTrue(exception.getMessage().contains("Order completed 25 hours ago"));
        assertTrue(exception.getMessage().contains("Cancellation window is 24 hours"));

        // Verify: Order should not be saved (status unchanged)
        assertEquals(OrderStatus.COMPLETED, testOrder.getStatus());
        verify(orderRepository, never()).save(testOrder);
    }

    @Test
    void testCancelAlreadyCancelledOrder() {
        // Setup: Order is already cancelled
        testOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(testOrder));

        // Execute & Verify: Should throw IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(1L));

        assertEquals("Cannot cancel order with status: CANCELLED", exception.getMessage());
    }

    @Test
    void testCancelProcessingOrder() {
        // Setup: Order is in PROCESSING status
        testOrder.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(testOrder));

        // Execute: Should not throw exception
        assertDoesNotThrow(() -> orderService.cancelOrder(1L));

        // Verify: Order status should be updated to CANCELLED
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void testCancelNonExistentOrder() {
        // Setup: Order not found
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        // Execute & Verify: Should throw OrderNotFoundException
        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                () -> orderService.cancelOrder(1L));

        assertEquals("Order not found with ID: 1", exception.getMessage());
    }
}
