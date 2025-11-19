package com.onlineshop.order.saga;

import com.onlineshop.order.communication.CommunicationStrategy;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementation of compensation handling
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompensationHandlerImpl implements CompensationHandler {
    
    private final CommunicationStrategy communicationStrategy;
    private final SagaStateRepository sagaStateRepository;
    
    @Override
    public void compensateInventory(Order order) {
        // TODO: Release reserved inventory
    }
    
    @Override
    public void compensatePayment(Order order) {
        // TODO: Refund payment
    }
    
    @Override
    public void compensateShipping(Order order) {
        // TODO: Cancel shipping
    }
    
    @Override
    public void executeCompensation(Order order) {
        // TODO: Execute compensation in reverse order
    }
}
