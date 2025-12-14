package com.onlineshop.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onlineshop.order.dto.request.OrderRequest;
import com.onlineshop.order.dto.response.OrderResponse;
import com.onlineshop.order.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for Order operations
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Received order creation request for customer: {}", request.getCustomerId());
        OrderResponse orderResponse = orderService.createOrder(request);
        log.info("Order created successfully with number: {}", orderResponse.getOrderNumber());
        return ResponseEntity.ok(orderResponse);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        log.info("Retrieving order with ID: {}", orderId);
        OrderResponse orderResponse = orderService.getOrderById(orderId);
        log.info("Order retrieved successfully: {}", orderResponse.getOrderNumber());
        return ResponseEntity.ok(orderResponse);
    }
    
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        log.info("Retrieving order with number: {}", orderNumber);
        OrderResponse orderResponse = orderService.getOrderByNumber(orderNumber);
        log.info("Order retrieved successfully: {}", orderResponse.getOrderNumber());
        return ResponseEntity.ok(orderResponse);
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomerId(@PathVariable Long customerId) {
        log.info("Retrieving orders for customer: {}", customerId);
        List<OrderResponse> orders = orderService.getOrdersByCustomerId(customerId);
        log.info("Retrieved {} orders for customer: {}", orders.size(), customerId);
        return ResponseEntity.ok(orders);
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        log.info("Cancelling order with ID: {}", orderId);
        orderService.cancelOrder(orderId);
        log.info("Order cancelled successfully: {}", orderId);
        return ResponseEntity.noContent().build();
    }
}
