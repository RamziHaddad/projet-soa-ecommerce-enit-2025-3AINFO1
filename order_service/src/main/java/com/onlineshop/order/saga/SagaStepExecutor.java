package com.onlineshop.order.saga;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.onlineshop.order.communication.OrderProcessingCommunicationHandler;
import com.onlineshop.order.exception.InventoryReservationException;
import com.onlineshop.order.exception.PaymentProcessingException;
import com.onlineshop.order.exception.ShippingArrangementException;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.SagaStep;
import com.onlineshop.order.repository.OrderRepository;
import com.onlineshop.order.saga.compensation.CompensationService;
import com.onlineshop.order.utils.RequestMapperService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for executing individual saga steps asynchronously.
 * Each step runs in its own transaction (REQUIRES_NEW) to ensure proper
 * transaction boundaries and state isolation between steps.
 * 
 * This design ensures:
 * - Each step commits independently before the next begins
 * - Failures in later steps don't rollback earlier committed steps
 * - Compensation can access committed state from completed steps
 * - Retry mechanism can resume from the correct step
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaStepExecutor {

    private static final String GENERAL_ERROR_MESSAGE = "Unknown error";
    private final OrderProcessingCommunicationHandler orderServiceCommunication;
    private final SagaStateService sagaStateService;
    private final OrderRepository orderRepository;
    private final RequestMapperService requestMapperService;
    private final CompensationService compensationService;

    /**
     * Executes the inventory reservation step asynchronously.
     * Runs in a new transaction that commits independently.
     * On success, triggers the next step (payment processing).
     * On failure, marks saga as failed and triggers compensation.
     *
     * @param orderId The ID of the order being processed
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeInventoryStep(Long orderId) {
        Order order = getOrder(orderId);
        log.info("Executing inventory reservation for order: {}", order.getOrderNumber());

        try {
            var inventoryRequest = requestMapperService.mapToInventoryRequest(order);
            var inventoryResponse = orderServiceCommunication.reserveInventory(inventoryRequest);

if (inventoryResponse != null && inventoryResponse.isSuccess()) {
                sagaStateService.updateInventoryStateAndProceed(order,
                        inventoryResponse.getOrderId(),
                        SagaStep.PAYMENT_PROCESSING);

                // Trigger next step asynchronously
                executePaymentStep(orderId);
            } else {
                handleStepFailure(order, false,
                        new InventoryReservationException("Inventory reservation failed: " +
                                (inventoryResponse != null ? inventoryResponse.getMessage() : GENERAL_ERROR_MESSAGE)));
            }
        } catch (Exception e) {
            handleStepFailure(order, false, e);
        }
    }

    /**
     * Executes the payment processing step asynchronously.
     * Runs in a new transaction that commits independently.
     * On success, triggers the next step (shipping arrangement).
     * On failure, marks saga as failed and triggers compensation.
     *
     * @param orderId The ID of the order being processed
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executePaymentStep(Long orderId) {
        Order order = getOrder(orderId);
        log.info("Executing payment processing for order: {}", order.getOrderNumber());

        try {
            var paymentRequest = requestMapperService.mapToPaymentRequest(order);
            var paymentResponse = orderServiceCommunication.processPayment(paymentRequest);

            if (paymentResponse != null && Boolean.TRUE.equals(paymentResponse.getSuccess())) {
                sagaStateService.updatePaymentStateAndProceed(order,
                        paymentResponse.getTransactionId(),
                        SagaStep.SHIPPING_ARRANGEMENT);

                // Trigger next step asynchronously
                executeShippingStep(orderId);
            } else {
                handleStepFailure(order, paymentResponse != null ? paymentResponse.getRetryable() : false,
                        new PaymentProcessingException("Payment processing failed: " +
                                (paymentResponse != null ? paymentResponse.getMessage() : GENERAL_ERROR_MESSAGE)));
            }
        } catch (Exception e) {
            handleStepFailure(order, false, e);
        }
    }

    /**
     * Executes the shipping arrangement step asynchronously.
     * Runs in a new transaction that commits independently.
     * On success, completes the order.
     * On failure, marks saga as failed and triggers compensation.
     *
     * @param orderId The ID of the order being processed
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeShippingStep(Long orderId) {
        Order order = getOrder(orderId);
        log.info("Executing shipping arrangement for order: {}", order.getOrderNumber());

        try {
            var shippingRequest = requestMapperService.mapToShippingRequest(order);
            var shippingResponse = orderServiceCommunication.arrangeShipping(shippingRequest);

            if (shippingResponse != null && Boolean.TRUE.equals(shippingResponse.getSuccess())) {
                sagaStateService.updateShippingStateAndProceed(order,
                        shippingResponse.getTrackingNumber(),
                        SagaStep.COMPLETED);

                // Complete the order
                completeOrder(orderId);
            } else {
                handleStepFailure(order, shippingResponse != null ? shippingResponse.getRetryable() : false,
                        new ShippingArrangementException("Shipping arrangement failed: " +
                                (shippingResponse != null ? shippingResponse.getMessage() : GENERAL_ERROR_MESSAGE)));
            }
        } catch (Exception e) {
            handleStepFailure(order, false, e);
        }
    }

    /**
     * Completes the order asynchronously.
     * Runs in a new transaction that commits independently.
     *
     * @param orderId The ID of the order being processed
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeOrder(Long orderId) {
        Order order = getOrder(orderId);
        log.info("Completing order: {}", order.getOrderNumber());

        try {
            
            sagaStateService.completeOrderAndSaga(order);
            log.info("Order completed successfully: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to complete order: {}", order.getOrderNumber(), e);
            handleStepFailure(order, false, e);
        }
    }

    /**
     * Handles step failure by updating saga state and triggering compensation.
     * Runs in the same transaction as the failed step.
     *
     * @param order     The order that failed
     * @param retryable Whether the failure is retryable
     * @param exception The exception that caused the failure
     */
    private void handleStepFailure(Order order, boolean retryable, Exception exception) {
        log.error("SAGA step failed for order: {}", order.getOrderNumber(), exception);

        try {
            // Update retryable state in a new transaction
            sagaStateService.updateRetryableState(order, retryable);

            // Mark saga as failed in a new transaction
            sagaStateService.failOrderAndSaga(order, exception);

            // Trigger compensation in a new transaction
            compensationService.compensateAfterFailure(order);
        } catch (Exception e) {
            log.error("Failed to handle step failure for order: {}", order.getOrderNumber(), e);
        }
    }

    /**
     * Retrieves an order by ID.
     * Uses a fresh query to get the latest state from the database.
     *
     * @param orderId The order ID
     * @return The order
     * @throws RuntimeException if order not found
     */
    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
    }
}
