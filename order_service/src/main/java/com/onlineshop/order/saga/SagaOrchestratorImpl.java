package com.onlineshop.order.saga;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlineshop.order.communication.OrderProcessingCommunicationHandler;
import com.onlineshop.order.exception.InventoryReservationException;
import com.onlineshop.order.exception.PaymentProcessingException;
import com.onlineshop.order.exception.ShippingArrangementException;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.OrderStatus;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.model.SagaStatus;
import com.onlineshop.order.model.SagaStep;
import com.onlineshop.order.repository.OrderRepository;
import com.onlineshop.order.repository.SagaStateRepository;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of SAGA orchestration pattern
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorImpl implements SagaOrchestrator {

    private final OrderProcessingCommunicationHandler orderServiceCommunication;
    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final CompensationHandler compensationHandler;

    @Resource
    private SagaOrchestratorImpl self;

    @Override
    @Transactional
    public void startSaga(Order order) {
        log.info("Starting SAGA for order: {}", order.getOrderNumber());

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

        try {
            self.executeNextStep(order);
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
                case ORDER_CREATED, INVENTORY_VALIDATION:
                    executeInventoryStep(order);
                    break;
                case PAYMENT_PROCESSING:
                    executePaymentStep(order);
                    break;
                case SHIPPING_ARRANGEMENT:
                    executeShippingStep(order);
                    break;
                case ORDER_CONFIRMATION, COMPLETED:
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

        return sagaState.getStatus() == SagaStatus.FAILED &&
                sagaState.getCurrentStep() != SagaStep.COMPLETED;
    }

    private void executeInventoryStep(Order order) {
        log.info("Executing inventory reservation for order: {}", order.getOrderNumber());

        try {

            var inventoryRequest = createInventoryRequest(order);

            var inventoryResponse = orderServiceCommunication.reserveInventory(inventoryRequest);

            if (inventoryResponse != null && Boolean.TRUE.equals(inventoryResponse.getSuccess())) {

                SagaState sagaState = getSagaState(order);
                sagaState.setInventoryReserved(true);
                sagaState.setInventoryTransactionId(inventoryResponse.getTransactionId());
                sagaStateRepository.save(sagaState);

                updateSagaStep(order, SagaStep.PAYMENT_PROCESSING);
                self.executeNextStep(order);
            } else {
                throw new InventoryReservationException("Inventory reservation failed");
            }
        } catch (Exception e) {
            log.error("Inventory step failed for order: {}", order.getOrderNumber(), e);
            throw e;
        }
    }

    private void executePaymentStep(Order order) {
        log.info("Executing payment processing for order: {}", order.getOrderNumber());

        try {

            var paymentRequest = createPaymentRequest(order);

            var paymentResponse = orderServiceCommunication.processPayment(paymentRequest);

            if (paymentResponse != null && Boolean.TRUE.equals(paymentResponse.getSuccess())) {

                SagaState sagaState = getSagaState(order);
                sagaState.setPaymentProcessed(true);
                sagaState.setPaymentTransactionId(paymentResponse.getTransactionId());
                sagaStateRepository.save(sagaState);

                updateSagaStep(order, SagaStep.SHIPPING_ARRANGEMENT);
                self.executeNextStep(order);
            } else {
                throw new PaymentProcessingException("Payment processing failed");
            }
        } catch (Exception e) {
            log.error("Payment step failed for order: {}", order.getOrderNumber(), e);

            self.compensate(order);
            throw e;
        }
    }

    private void executeShippingStep(Order order) {
        log.info("Executing shipping arrangement for order: {}", order.getOrderNumber());

        try {

            var shippingRequest = createShippingRequest(order);

            var shippingResponse = orderServiceCommunication.arrangeShipping(shippingRequest);

            if (shippingResponse != null && Boolean.TRUE.equals(shippingResponse.getSuccess())) {

                SagaState sagaState = getSagaState(order);
                sagaState.setShippingArranged(true);
                sagaState.setShippingTransactionId(shippingResponse.getTrackingNumber());
                sagaStateRepository.save(sagaState);

                updateSagaStep(order, SagaStep.COMPLETED);
                self.executeNextStep(order);
            } else {
                throw new ShippingArrangementException("Shipping arrangement failed");
            }
        } catch (Exception e) {
            log.error("Shipping step failed for order: {}", order.getOrderNumber(), e);

            self.compensate(order);
            throw e;
        }
    }

    private void completeOrder(Order order) {
        log.info("Completing order: {}", order.getOrderNumber());

        order.setStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

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
        return com.onlineshop.order.dto.request.InventoryRequest.builder()
                .orderNumber(order.getOrderNumber())
                .items(order.getItems().stream()
                        .map(item -> com.onlineshop.order.dto.request.InventoryItemRequest.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();
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
