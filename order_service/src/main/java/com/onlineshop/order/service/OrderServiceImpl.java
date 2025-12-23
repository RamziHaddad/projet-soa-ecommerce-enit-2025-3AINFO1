package com.onlineshop.order.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlineshop.order.dto.request.OrderRequest;
import com.onlineshop.order.dto.response.OrderResponse;
import com.onlineshop.order.exception.OrderNotFoundException;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.OrderItem;
import com.onlineshop.order.config.OrderServiceConfig;
import com.onlineshop.order.model.OrderStatus;
import com.onlineshop.order.repository.OrderRepository;
import com.onlineshop.order.saga.SagaOrchestrator;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of Order service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final SagaOrchestrator sagaOrchestrator;
    private final OrderServiceConfig orderServiceConfig;
    
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());
        
        
        String orderNumber = generateOrderNumber();
        
        
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerId(request.getCustomerId())
                .status(OrderStatus.PROCESSING)
                .shippingAddress(request.getShippingAddress())
                .build();
        
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var itemRequest : request.getItems()) {
            BigDecimal itemSubtotal = itemRequest.getUnitPrice()
                .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem item = OrderItem.builder()
                .order(order)
                .productId(itemRequest.getProductId())
                .quantity(itemRequest.getQuantity())
                .unitPrice(itemRequest.getUnitPrice())
                .subtotal(itemSubtotal)
                .build();

            order.getItems().add(item);
            totalAmount = totalAmount.add(itemSubtotal);
        }
        order.setTotalAmount(totalAmount);
        
        
        Order savedOrder = orderRepository.save(order);
        
        
        try {
            sagaOrchestrator.startSaga(savedOrder);
            log.info("SAGA workflow started for order: {}", orderNumber);
        } catch (Exception e) {
            log.error("Failed to start SAGA for order: {}", orderNumber, e);
            
            savedOrder.setStatus(OrderStatus.FAILED);
            orderRepository.save(savedOrder);
            throw new RuntimeException("Failed to start order processing", e);
        }
        
        return mapToResponse(savedOrder);
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(@NonNull Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        return mapToResponse(order);
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(@NonNull String orderNumber) {
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
    public void cancelOrder(@NonNull Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        // Cannot cancel already cancelled orders
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel order with status: " + order.getStatus());
        }

        // For completed orders, check if within cancellation window
        if (order.getStatus() == OrderStatus.COMPLETED) {
            LocalDateTime completedTime = order.getUpdatedAt();
            Duration timeSinceCompletion = Duration.between(completedTime, LocalDateTime.now());
            long hoursSinceCompletion = timeSinceCompletion.toHours();

            if (hoursSinceCompletion > orderServiceConfig.getCancellationWindowHours()) {
                throw new IllegalStateException(
                    "Order completed " + hoursSinceCompletion + " hours ago. " +
                    "Cancellation window is " + orderServiceConfig.getCancellationWindowHours() + " hours."
                );
            }
            log.info("Allowing cancellation of completed order within time window. Order: {}, Completed: {} hours ago",
                    order.getOrderNumber(), hoursSinceCompletion);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        if (order.getStatus() != OrderStatus.PENDING) {
            try {
                sagaOrchestrator.compensate(order);
                log.info("Compensation triggered for order: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Failed to compensate order: {}", order.getOrderNumber(), e);
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
                    .subtotal(item.getSubtotal())
                    .build())
                .collect(Collectors.toList()))
            .build();
        }
}
