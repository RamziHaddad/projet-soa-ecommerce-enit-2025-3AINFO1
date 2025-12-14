package com.onlineshop.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onlineshop.order.dto.request.OrderItemRequest;
import com.onlineshop.order.dto.request.OrderRequest;
import com.onlineshop.order.dto.response.OrderResponse;
import com.onlineshop.order.exception.OrderNotFoundException;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.OrderItem;
import com.onlineshop.order.model.OrderStatus;
import com.onlineshop.order.repository.OrderRepository;
import com.onlineshop.order.saga.SagaOrchestrator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SagaOrchestrator sagaOrchestrator;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderRequest orderRequest;
    private Order testOrder;
    private OrderResponse expectedResponse;

    @BeforeEach
    void setUp() {
        // Initialize test data
        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .build();
        
        orderRequest = OrderRequest.builder()
                .customerId(1L)
                .shippingAddress("123 Main St, City, State 12345")
                .items(Arrays.asList(itemRequest))
                .build();
        
        // Create test order
        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .productId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .build();
        
        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-20250114120000-123")
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("59.98"))
                .shippingAddress("123 Main St, City, State 12345")
                .items(Arrays.asList(orderItem))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Create expected response
        expectedResponse = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-20250114120000-123")
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("59.98"))
                .shippingAddress("123 Main St, City, State 12345")
                .items(Arrays.asList(com.onlineshop.order.dto.response.OrderItemResponse.builder()
                        .id(1L)
                        .productId(1L)
                        .quantity(2)
                        .unitPrice(new BigDecimal("29.99"))
                        .build()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateOrder() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(sagaOrchestrator).startSaga(any(Order.class));

        // When
        OrderResponse result = orderService.createOrder(orderRequest);

        // Then
        assertNotNull(result);
        assertEquals(testOrder.getCustomerId(), result.getCustomerId());
        assertEquals(testOrder.getStatus(), result.getStatus());
        assertEquals(new BigDecimal("59.98"), result.getTotalAmount());
        
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(sagaOrchestrator, times(1)).startSaga(any(Order.class));
    }

    @Test
    void testCreateOrderSagaFailure() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doThrow(new RuntimeException("SAGA failed")).when(sagaOrchestrator).startSaga(any(Order.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(orderRequest);
        });
        
        // Verify order status is updated to FAILED
        verify(orderRepository, times(2)).save(any(Order.class)); // Once for initial save, once for status update
        verify(sagaOrchestrator, times(1)).startSaga(any(Order.class));
    }

    @Test
    void testGetOrderById() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        OrderResponse result = orderService.getOrderById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testOrder.getId(), result.getId());
        assertEquals(testOrder.getOrderNumber(), result.getOrderNumber());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testGetOrderByIdNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderById(999L);
        });
        verify(orderRepository, times(1)).findById(999L);
    }

    @Test
    void testGetOrderByNumber() {
        // Given
        when(orderRepository.findByOrderNumber("ORD-20250114120000-123")).thenReturn(Optional.of(testOrder));

        // When
        OrderResponse result = orderService.getOrderByNumber("ORD-20250114120000-123");

        // Then
        assertNotNull(result);
        assertEquals(testOrder.getOrderNumber(), result.getOrderNumber());
        verify(orderRepository, times(1)).findByOrderNumber("ORD-20250114120000-123");
    }

    @Test
    void testGetOrderByNumberNotFound() {
        // Given
        when(orderRepository.findByOrderNumber("NON-EXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderByNumber("NON-EXISTENT");
        });
        verify(orderRepository, times(1)).findByOrderNumber("NON-EXISTENT");
    }

    @Test
    void testGetOrdersByCustomerId() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findByCustomerId(1L)).thenReturn(orders);

        // When
        List<OrderResponse> result = orderService.getOrdersByCustomerId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder.getCustomerId(), result.get(0).getCustomerId());
        verify(orderRepository, times(1)).findByCustomerId(1L);
    }

    @Test
    void testCancelOrder() {
        // Given
        testOrder.setStatus(OrderStatus.INVENTORY_RESERVED); // Set to a cancellable status
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(sagaOrchestrator).compensate(any(Order.class));

        // When
        orderService.cancelOrder(1L);

        // Then
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(testOrder);
        verify(sagaOrchestrator, times(1)).compensate(testOrder);
    }

    @Test
    void testCancelOrderAlreadyCompleted() {
        // Given
        testOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(1L);
        });
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCancelOrderAlreadyCancelled() {
        // Given
        testOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(1L);
        });
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCancelOrderNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.cancelOrder(999L);
        });
        verify(orderRepository, times(1)).findById(999L);
    }

    @Test
    void testCancelOrderPendingStatus() {
        // Given
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(sagaOrchestrator).compensate(any(Order.class));

        // When
        orderService.cancelOrder(1L);

        // Then
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        // After cancelling, compensation is triggered because status is no longer PENDING
        verify(sagaOrchestrator, times(1)).compensate(testOrder);
    }
}
