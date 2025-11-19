package com.onlineshop.order.service;

import com.onlineshop.order.dto.request.OrderRequest;
import com.onlineshop.order.dto.response.OrderResponse;
import com.onlineshop.order.repository.OrderRepository;
import com.onlineshop.order.saga.SagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        // TODO: Create order entity, validate, and start SAGA
        return null;
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        // TODO: Retrieve order by ID
        return null;
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        // TODO: Retrieve order by order number
        return null;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        // TODO: Retrieve all orders for customer
        return null;
    }
    
    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        // TODO: Cancel order and trigger compensation
    }
    
    private String generateOrderNumber() {
        // TODO: Generate unique order number
        return null;
    }
}
