package com.onlineshop.order.saga;

import com.onlineshop.order.communication.CommunicationStrategy;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.OrderStatus;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.model.SagaStatus;
import com.onlineshop.order.model.SagaStep;
import com.onlineshop.order.repository.OrderRepository;
import com.onlineshop.order.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of SAGA orchestration pattern
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorImpl implements SagaOrchestrator {
    
    private final CommunicationStrategy communicationStrategy;
    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final CompensationHandler compensationHandler;
    
    @Override
    @Transactional
    public void startSaga(Order order) {
        log.info("Starting SAGA for order: {}", order.getOrderNumber());
        
        // Initialize SAGA state
        SagaState sagaState = SagaState.builder()
                .order(order)
                .status(SagaStatus.STARTED)
                .currentStep(SagaStep.ORDER_CREATED)
                .inventoryReserved(false)
                .paymentProcessed(false)
                .shippingArranged(false)
                .retryCount(0)
                .build();
        
        sagaStateRepository.save(sagaState);
        
        // Start workflow execution
        try {
            executeNextStep(order);
        } catch (Exception e) {
            log.error("SAGA execution failed for order: {}", order.getOrderNumber(), e);
            handleSagaFailure(order, e);
        }
    }
    
    @Override
    @Transactional
    public void executeNextStep(Order order) {
        SagaState sagaState = getSagaState(order);
        log.info("Executing SAGA step: {} for order: {}", sagaState.getCurrentStep(), order.getOrderNumber());
        
        try {
            switch (sagaState.getCurrentStep()) {
                case ORDER_CREATED:
                case INVENTORY_VALIDATION:
                    executeInventoryStep(order);
                    break;
                case PAYMENT_PROCESSING:
                    executePaymentStep(order);
                    break;
                case SHIPPING_ARRANGEMENT:
                    executeShippingStep(order);
                    break;
                case ORDER_CONFIRMATION:
                case COMPLETED:
                    completeOrder(order);
                    break;
                default:
                    throw new IllegalStateException("Unknown SAGA step: " + sagaState.getCurrentStep());
            }
        } catch (Exception e) {
            log.error("SAGA step failed: {} for order: {}", sagaState.getCurrentStep(), order.getOrderNumber(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public void compensate(Order order) {
        log.info("Starting compensation for order: {}", order.getOrderNumber());
        
        SagaState sagaState = getSagaState(order);
        sagaState.setStatus(SagaStatus.COMPENSATING);
        sagaStateRepository.save(sagaState);
        
        try {
            compensationHandler.executeCompensation(order);
            sagaState.setStatus(SagaStatus.COMPENSATED);
            sagaStateRepository.save(sagaState);
            log.info("Compensation completed for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Compensation failed for order: {}", order.getOrderNumber(), e);
            sagaState.setStatus(SagaStatus.FAILED);
            sagaStateRepository.save(sagaState);
            throw e;
        }
    }
    
    @Override
    public boolean canRetry(Order order) {
        SagaState sagaState = getSagaState(order);
        // Can retry if saga failed but not compensated and step is retryable
        return sagaState.getStatus() == SagaStatus.FAILED && 
               sagaState.getCurrentStep() != SagaStep.COMPLETED;
    }
    
    private void executeInventoryStep(Order order) {
        log.info("Executing inventory reservation for order: {}", order.getOrderNumber());
        
        try {
            // Create inventory request
            var inventoryRequest = createInventoryRequest(order);
            
            // Call inventory service
            var inventoryResponse = communicationStrategy.reserveInventory(inventoryRequest);
            
            if (inventoryResponse != null && Boolean.TRUE.equals(inventoryResponse.getSuccess())) {
                // Update saga state
                SagaState sagaState = getSagaState(order);
                sagaState.setInventoryReserved(true);
                sagaState.setInventoryTransactionId(inventoryResponse.getTransactionId());
                sagaStateRepository.save(sagaState);
                
                // Move to next step
                updateSagaStep(order, SagaStep.PAYMENT_PROCESSING);
                executeNextStep(order);
            } else {
                throw new RuntimeException("Inventory reservation failed");
            }
        } catch (Exception e) {
            log.error("Inventory step failed for order: {}", order.getOrderNumber(), e);
            throw e;
        }
    }
    
    private void executePaymentStep(Order order) {
        log.info("Executing payment processing for order: {}", order.getOrderNumber());
        
        try {
            // Create payment request
            var paymentRequest = createPaymentRequest(order);
            
            // Call payment service
            var paymentResponse = communicationStrategy.processPayment(paymentRequest);
            
            if (paymentResponse != null && Boolean.TRUE.equals(paymentResponse.getSuccess())) {
                // Update saga state
                SagaState sagaState = getSagaState(order);
                sagaState.setPaymentProcessed(true);
                sagaState.setPaymentTransactionId(paymentResponse.getTransactionId());
                sagaStateRepository.save(sagaState);
                
                // Move to next step
                updateSagaStep(order, SagaStep.SHIPPING_ARRANGEMENT);
                executeNextStep(order);
            } else {
                throw new RuntimeException("Payment processing failed");
            }
        } catch (Exception e) {
            log.error("Payment step failed for order: {}", order.getOrderNumber(), e);
            // Trigger compensation for previous steps
            compensate(order);
            throw e;
        }
    }
    
    private void executeShippingStep(Order order) {
        log.info("Executing shipping arrangement for order: {}", order.getOrderNumber());
        
        try {
            // Create shipping request
            var shippingRequest = createShippingRequest(order);
            
            // Call shipping service
            var shippingResponse = communicationStrategy.arrangeShipping(shippingRequest);
            
            if (shippingResponse != null && Boolean.TRUE.equals(shippingResponse.getSuccess())) {
                // Update saga state
                SagaState sagaState = getSagaState(order);
                sagaState.setShippingArranged(true);
                sagaState.setShippingTransactionId(shippingResponse.getTrackingNumber());
                sagaStateRepository.save(sagaState);
                
                // Move to completion step
                updateSagaStep(order, SagaStep.COMPLETED);
                executeNextStep(order);
            } else {
                throw new RuntimeException("Shipping arrangement failed");
            }
        } catch (Exception e) {
            log.error("Shipping step failed for order: {}", order.getOrderNumber(), e);
            // Trigger compensation for previous steps
            compensate(order);
            throw e;
        }
    }
    
    private void completeOrder(Order order) {
        log.info("Completing order: {}", order.getOrderNumber());
        
        order.setStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        
        // Update saga state
        SagaState sagaState = getSagaState(order);
        sagaState.setStatus(SagaStatus.COMPLETED);
        sagaStateRepository.save(sagaState);
        
        log.info("Order completed successfully: {}", order.getOrderNumber());
    }
    
    private void handleSagaFailure(Order order, Exception e) {
        order.setStatus(OrderStatus.FAILED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        
        SagaState sagaState = getSagaState(order);
        sagaState.setStatus(SagaStatus.FAILED);
        sagaState.setErrorMessage(e.getMessage());
        sagaStateRepository.save(sagaState);
    }
    
    private SagaState getSagaState(Order order) {
        return sagaStateRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Saga state not found for order: " + order.getOrderNumber()));
    }
    
    private void updateSagaStep(Order order, SagaStep nextStep) {
        SagaState sagaState = getSagaState(order);
        sagaState.setCurrentStep(nextStep);
        sagaState.setStatus(SagaStatus.IN_PROGRESS);
        sagaStateRepository.save(sagaState);
    }
    
    private com.onlineshop.order.dto.request.InventoryRequest createInventoryRequest(Order order) {
        var inventoryRequest = com.onlineshop.order.dto.request.InventoryRequest.builder()
                .orderNumber(order.getOrderNumber())
                .items(order.getItems().stream()
                        .map(item -> com.onlineshop.order.dto.request.InventoryItemRequest.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();
        return inventoryRequest;
    }
    
    private com.onlineshop.order.dto.request.PaymentRequest createPaymentRequest(Order order) {
        return com.onlineshop.order.dto.request.PaymentRequest.builder()
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .amount(order.getTotalAmount())
                .build();
    }
    
    private com.onlineshop.order.dto.request.ShippingRequest createShippingRequest(Order order) {
        return com.onlineshop.order.dto.request.ShippingRequest.builder()
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .shippingAddress(order.getShippingAddress())
                .build();
    }
}
