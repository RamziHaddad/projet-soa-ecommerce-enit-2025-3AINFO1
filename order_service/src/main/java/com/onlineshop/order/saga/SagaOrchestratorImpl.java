package com.onlineshop.order.saga;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlineshop.order.communication.OrderProcessingCommunicationHandler;
import com.onlineshop.order.exception.InventoryReservationException;
import com.onlineshop.order.exception.PaymentProcessingException;
import com.onlineshop.order.exception.ShippingArrangementException;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.model.SagaStatus;
import com.onlineshop.order.model.SagaStep;
import com.onlineshop.order.repository.SagaStateRepository;
import com.onlineshop.order.saga.compensation.CompensationService;
import com.onlineshop.order.saga.retry.RetryService;
import com.onlineshop.order.utils.RequestMapperService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of SAGA orchestration pattern.
 * Refactored to use SagaStateService for batched state updates and
 * RequestMapperService
 * for request mapping, reducing database round-trips from 9+ to 3 per
 * successful saga execution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorImpl implements SagaOrchestrator {

    private static final String GENERAL_ERROR_MESSAGE = "Unknown error";
    private final OrderProcessingCommunicationHandler orderServiceCommunication;
    private final SagaStateRepository sagaStateRepository;
    private final CompensationService compensationService;
    private final RetryService retryService;
    private final SagaStateService sagaStateService;
    private final RequestMapperService requestMapperService;

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
                .maxRetries(retryService.getMaxRetries())
                .retryable(true)
                .build();

        sagaStateRepository.save(sagaState);

        try {
            executeNextStep(order);
        } catch (Exception e) {
            log.error("SAGA execution failed for order: {}", order.getOrderNumber(), e);

            sagaStateService.failOrderAndSaga(order, e);

            compensationService.compensateAfterFailure(order);
        }
    }

    @Override
    public void executeNextStep(Order order) {
        SagaState sagaState = getSagaState(order);
        log.info("Executing SAGA step: {} for order: {}", sagaState.getCurrentStep(), order.getOrderNumber());

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
    }

    @Override
    @Transactional
    public void compensate(Order order) {

        compensationService.compensateAfterFailure(order);
    }

    @Override
    public boolean canRetry(Order order) {
        SagaState sagaState = getSagaState(order);

        return retryService.canRetry(sagaState) && sagaState.getRetryable();
    }

    @Override
    @Transactional
    public void retrySaga(Order order) {
        log.info("Attempting to retry SAGA for order: {}", order.getOrderNumber());

        SagaState sagaState = getSagaState(order);

        if (!canRetry(order)) {
            log.warn("Cannot retry SAGA for order {} - not retryable or max retries exceeded", order.getOrderNumber());
            return;
        }

        try {
            retryService.prepareForRetry(sagaState);
            sagaState.setRetryable(true);
            sagaStateRepository.save(sagaState);

            executeNextStep(order);
            log.info("SAGA retry initiated successfully for order: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("SAGA retry failed for order: {}", order.getOrderNumber(), e);
            retryService.markRetryFailed(sagaState, e);

            throw e;
        }
    }

    private void executeInventoryStep(Order order) {
        log.info("Executing inventory reservation for order: {}", order.getOrderNumber());

        var inventoryRequest = requestMapperService.mapToInventoryRequest(order);
        var inventoryResponse = orderServiceCommunication.reserveInventory(inventoryRequest);

        if (inventoryResponse != null) {
            if (Boolean.TRUE.equals(inventoryResponse.getSuccess())) {
                sagaStateService.updateInventoryStateAndProceed(order,
                        inventoryResponse.getTransactionId(),
                        SagaStep.PAYMENT_PROCESSING);
                executeNextStep(order);
            } else {
                sagaStateService.updateRetryableState(order, inventoryResponse.getRetryable());
                throw new InventoryReservationException("Inventory reservation failed: " +
                        (inventoryResponse != null ? inventoryResponse.getMessage() : GENERAL_ERROR_MESSAGE));
            }
        }
    }

    private void executePaymentStep(Order order) {
        log.info("Executing payment processing for order: {}", order.getOrderNumber());

        var paymentRequest = requestMapperService.mapToPaymentRequest(order);
        var paymentResponse = orderServiceCommunication.processPayment(paymentRequest);

        if (paymentResponse != null) {
            if (Boolean.TRUE.equals(paymentResponse.getSuccess())) {
                sagaStateService.updatePaymentStateAndProceed(order,
                        paymentResponse.getTransactionId(),
                        SagaStep.SHIPPING_ARRANGEMENT);
                executeNextStep(order);
            } else {
                sagaStateService.updateRetryableState(order, paymentResponse.getRetryable());
                throw new PaymentProcessingException("Payment processing failed: " +
                        (paymentResponse != null ? paymentResponse.getMessage() : GENERAL_ERROR_MESSAGE));
            }
        }
    }

    private void executeShippingStep(Order order) {
        log.info("Executing shipping arrangement for order: {}", order.getOrderNumber());

        var shippingRequest = requestMapperService.mapToShippingRequest(order);
        var shippingResponse = orderServiceCommunication.arrangeShipping(shippingRequest);

        if (shippingResponse != null) {
            if (Boolean.TRUE.equals(shippingResponse.getSuccess())) {
                sagaStateService.updateShippingStateAndProceed(order,
                        shippingResponse.getTrackingNumber(),
                        SagaStep.COMPLETED);
                executeNextStep(order);
            } else {
                sagaStateService.updateRetryableState(order, shippingResponse.getRetryable());
                throw new ShippingArrangementException("Shipping arrangement failed: " +
                        (shippingResponse != null ? shippingResponse.getMessage() : GENERAL_ERROR_MESSAGE));
            }
        }
    }

    private void completeOrder(Order order) {
        log.info("Completing order: {}", order.getOrderNumber());

        sagaStateService.completeOrderAndSaga(order);

        log.info("Order completed successfully: {}", order.getOrderNumber());
    }

    private SagaState getSagaState(Order order) {
        return sagaStateRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Saga state not found for order: " + order.getOrderNumber()));
    }
}
