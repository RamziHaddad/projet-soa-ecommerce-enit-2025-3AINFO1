package com.onlineshop.order.service;

import com.onlineshop.order.dto.request.OrderRequest;
import com.onlineshop.order.dto.response.OrderResponse;

import java.util.List;

/**
 * Service interface for Order operations
 */
public interface OrderService {
    
    /**
     * Create a new order and start SAGA workflow
     * @param request Order creation request
     * @return Created order response
     */
    OrderResponse createOrder(OrderRequest request);
    
    /**
     * Get order by ID
     * @param orderId Order ID
     * @return Order response
     */
    OrderResponse getOrderById(Long orderId);
    
    /**
     * Get order by order number
     * @param orderNumber Order number
     * @return Order response
     */
    OrderResponse getOrderByNumber(String orderNumber);
    
    /**
     * Get all orders for a customer
     * @param customerId Customer ID
     * @return List of order responses
     */
    List<OrderResponse> getOrdersByCustomerId(Long customerId);
    
    /**
     * Cancel an order
     * @param orderId Order ID
     */
    void cancelOrder(Long orderId);
}
