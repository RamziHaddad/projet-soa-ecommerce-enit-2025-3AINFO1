package com.onlineshop.order.service;

import com.onlineshop.order.dto.request.OrderRequest;
import com.onlineshop.order.dto.response.OrderResponse;
import com.onlineshop.order.exception.OrderNotFoundException;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.OrderItem;
import com.onlineshop.order.model.OrderStatus;
import com.onlineshop.order.repository.OrderRepository;
import com.onlineshop.order.saga.SagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of Order service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final SagaOrchestrator sagaOrchestrator;
    
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());
        
        // Generate unique order number
        String orderNumber = generateOrderNumber();
        
        // Create order entity
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .build();
        
        // Calculate total amount and add items
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .build();
            order.getItems().add(item);
            totalAmount = totalAmount.add(
                itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
            );
        }
        order.setTotalAmount(totalAmount);
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        
        // Start SAGA workflow
        try {
            sagaOrchestrator.startSaga(savedOrder);
            log.info("SAGA workflow started for order: {}", orderNumber);
        } catch (Exception e) {
            log.error("Failed to start SAGA for order: {}", orderNumber, e);
            // Update order status to failed
            savedOrder.setStatus(OrderStatus.FAILED);
            orderRepository.save(savedOrder);
            throw new RuntimeException("Failed to start order processing", e);
        }
        
        return mapToResponse(savedOrder);
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        return mapToResponse(order);
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with number: " + orderNumber));
        return mapToResponse(order);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        
        // Check if order can be cancelled
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel order with status: " + order.getStatus());
        }
        
        // Update status to cancelled
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        
        // Trigger compensation if needed
        if (order.getStatus() != OrderStatus.PENDING) {
            try {
                sagaOrchestrator.compensate(order);
                log.info("Compensation triggered for order: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Failed to compensate order: {}", order.getOrderNumber(), e);
                // Don't throw exception here as order is already cancelled
            }
        }
        
        log.info("Order cancelled successfully: {}", order.getOrderNumber());
    }
    
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.valueOf((int) (Math.random() * 1000));
        return "ORD-" + timestamp + "-" + random;
    }
    
    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream()
                        .map(item -> com.onlineshop.order.dto.response.OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
