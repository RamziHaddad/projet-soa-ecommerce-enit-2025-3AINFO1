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
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.customerId());

        Long orderId = createOrderInTransaction(request);
        startSagaAsync(orderId);

        Order savedOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        return mapToResponse(savedOrder);
    }

    /**
     * Creates and saves the order in a dedicated transaction.
     * This transaction commits before the saga starts.
     *
     * @param request The order creation request
     * @return The ID of the created order
     */
    @Transactional
    protected Long createOrderInTransaction(OrderRequest request) {
        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerId(request.customerId())
                .status(OrderStatus.PROCESSING)
                .shippingAddress(request.shippingAddress())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var itemRequest : request.items()) {
            BigDecimal itemSubtotal = itemRequest.unitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(itemRequest.productId())
                    .quantity(itemRequest.quantity())
                    .unitPrice(itemRequest.unitPrice())
                    .subtotal(itemSubtotal)
                    .build();

            order.getItems().add(item);
            totalAmount = totalAmount.add(itemSubtotal);
        }
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        return savedOrder.getId();
    }

    /**
     * Starts the saga workflow asynchronously outside the order creation
     * transaction.
     * This ensures that the saga execution does not hold the order creation
     * transaction open.
     * Each saga step will execute in its own transaction.
     *
     * @param orderId The ID of the order to start the saga for
     */
    private void startSagaAsync(Long orderId) {
        try {
            // Fetch the order in a separate context
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

            sagaOrchestrator.startSaga(order);
            log.info("SAGA workflow initiated for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to start SAGA for order ID: {}", orderId, e);

            // Update order status to failed
            orderRepository.findById(orderId).ifPresent(order -> {
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
            });
        }
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
                                "Cancellation window is " + orderServiceConfig.getCancellationWindowHours()
                                + " hours.");
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
        var items = order.getItems().stream()
                .map(item -> new com.onlineshop.order.dto.response.OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        null,
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
