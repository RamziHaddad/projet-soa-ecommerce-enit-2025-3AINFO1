package com.onlineshop.order.saga;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.onlineshop.order.exception.SagaStateUpdateException;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.OrderStatus;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.model.SagaStatus;
import com.onlineshop.order.model.SagaStep;
import com.onlineshop.order.repository.OrderRepository;
import com.onlineshop.order.repository.SagaStateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for managing saga state updates.
 * Provides transactional methods for updating saga state, including batched
 * updates
 * to reduce database round-trips.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaStateService {

    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;

    /**
     * Updates inventory state and advances to the next step in a single
     * transaction.
     * Batches saga state update, step transition, and order status update.
     * Uses REQUIRES_NEW to ensure this update commits independently.
     *
     * @param order         The order being processed
     * @param transactionId The inventory transaction ID
     * @param nextStep      The next step to transition to
     * @throws SagaStateUpdateException if the update fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateInventoryStateAndProceed(Order order, String transactionId, SagaStep nextStep) {
        try {
            log.debug("Updating inventory state for order: {} with transaction: {}",
                    order.getOrderNumber(), transactionId);

            // Reload order in this new transaction to avoid detached entity issues
            Order managedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new SagaStateUpdateException(order.getOrderNumber(),
                            "Order not found"));

            SagaState sagaState = getSagaState(managedOrder);

            sagaState.setInventoryReserved(true);
            sagaState.setInventoryTransactionId(transactionId);
            sagaState.setCurrentStep(nextStep);
            sagaState.setStatus(SagaStatus.IN_PROGRESS);

            managedOrder.setStatus(OrderStatus.INVENTORY_RESERVED);
            sagaStateRepository.save(sagaState);
            orderRepository.save(managedOrder);

            log.debug("Successfully updated inventory state and proceeded to step: {} for order: {}",
                    nextStep, order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to update inventory state for order: {}", order.getOrderNumber(), e);
            throw new SagaStateUpdateException(order.getOrderNumber(),
                    "Failed to update inventory state", e);
        }
    }

    /**
     * Updates payment state and advances to the next step in a single transaction.
     * Batches saga state update and step transition.
     * Uses REQUIRES_NEW to ensure this update commits independently.
     *
     * @param order         The order being processed
     * @param transactionId The payment transaction ID
     * @param nextStep      The next step to transition to
     * @throws SagaStateUpdateException if the update fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePaymentStateAndProceed(Order order, String transactionId, SagaStep nextStep) {
        try {
            log.debug("Updating payment state for order: {} with transaction: {}",
                    order.getOrderNumber(), transactionId);

            // Reload order in this new transaction to avoid detached entity issues
            Order managedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new SagaStateUpdateException(order.getOrderNumber(),
                            "Order not found"));

            SagaState sagaState = getSagaState(managedOrder);

            sagaState.setPaymentProcessed(true);
            sagaState.setPaymentTransactionId(transactionId);
            sagaState.setCurrentStep(nextStep);
            sagaState.setStatus(SagaStatus.IN_PROGRESS);

            managedOrder.setStatus(OrderStatus.PAYMENT_PROCESSED);
            sagaStateRepository.save(sagaState);
            orderRepository.save(managedOrder);

            log.debug("Successfully updated payment state and proceeded to step: {} for order: {}",
                    nextStep, order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to update payment state for order: {}", order.getOrderNumber(), e);
            throw new SagaStateUpdateException(order.getOrderNumber(),
                    "Failed to update payment state", e);
        }
    }

    /**
     * Updates shipping state and advances to the next step in a single transaction.
     * Batches saga state update and step transition.
     * Uses REQUIRES_NEW to ensure this update commits independently.
     *
     * @param order          The order being processed
     * @param trackingNumber The shipping tracking number
     * @param nextStep       The next step to transition to
     * @throws SagaStateUpdateException if the update fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateShippingStateAndProceed(Order order, String trackingNumber, SagaStep nextStep) {
        try {
            log.debug("Updating shipping state for order: {} with tracking: {}",
                    order.getOrderNumber(), trackingNumber);

            // Reload order in this new transaction to avoid detached entity issues
            Order managedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new SagaStateUpdateException(order.getOrderNumber(),
                            "Order not found"));

            SagaState sagaState = getSagaState(managedOrder);

            sagaState.setShippingArranged(true);
            sagaState.setShippingTransactionId(trackingNumber);
            sagaState.setCurrentStep(nextStep);
            sagaState.setStatus(SagaStatus.IN_PROGRESS);

            managedOrder.setStatus(OrderStatus.SHIPPING_ARRANGED);
            sagaStateRepository.save(sagaState);
            orderRepository.save(managedOrder);

            log.debug("Successfully updated shipping state and proceeded to step: {} for order: {}",
                    nextStep, order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to update shipping state for order: {}", order.getOrderNumber(), e);
            throw new SagaStateUpdateException(order.getOrderNumber(),
                    "Failed to update shipping state", e);
        }
    }

    /**
     * Updates the retryable flag for a saga.
     * Used to mark whether a saga can be retried after a failure.
     * Uses REQUIRES_NEW to ensure this update commits independently.
     *
     * @param order     The order being processed
     * @param retryable Whether the saga can be retried
     * @throws SagaStateUpdateException if the update fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRetryableState(Order order, boolean retryable) {
        try {
            log.debug("Updating retryable state for order: {} to: {}", order.getOrderNumber(), retryable);

            // Reload order in this new transaction to avoid detached entity issues
            Order managedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new SagaStateUpdateException(order.getOrderNumber(),
                            "Order not found"));

            SagaState sagaState = getSagaState(managedOrder);
            sagaState.setRetryable(retryable);
            sagaStateRepository.save(sagaState);

            log.debug("Successfully updated retryable state for order: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to update retryable state for order: {}", order.getOrderNumber(), e);
            throw new SagaStateUpdateException(order.getOrderNumber(),
                    "Failed to update retryable state", e);
        }
    }

    /**
     * Completes the order and saga in a single transaction.
     * Updates both order status and saga status atomically.
     * Uses REQUIRES_NEW to ensure this update commits independently.
     *
     * @param order The order to complete
     * @throws SagaStateUpdateException if the update fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeOrderAndSaga(Order order) {
        try {
            log.debug("Completing order and saga for: {}", order.getOrderNumber());

            // Reload order in this new transaction to avoid detached entity issues
            Order managedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new SagaStateUpdateException(order.getOrderNumber(),
                            "Order not found"));

            managedOrder.setStatus(OrderStatus.COMPLETED);
            managedOrder.setUpdatedAt(LocalDateTime.now());

            SagaState sagaState = getSagaState(managedOrder);
            sagaState.setStatus(SagaStatus.COMPLETED);

            sagaStateRepository.save(sagaState);
            orderRepository.save(managedOrder);

            log.info("Successfully completed order and saga for: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to complete order and saga for: {}", order.getOrderNumber(), e);
            throw new SagaStateUpdateException(order.getOrderNumber(),
                    "Failed to complete order and saga", e);
        }
    }

    /**
     * Marks the order and saga as failed in a single transaction.
     * Updates both order status and saga status atomically.
     * Uses REQUIRES_NEW to ensure this update commits independently.
     *
     * @param order     The order that failed
     * @param exception The exception that caused the failure
     * @throws SagaStateUpdateException if the update fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failOrderAndSaga(Order order, Exception exception) {
        try {
            log.debug("Marking order and saga as failed for: {}", order.getOrderNumber());

            // Reload order in this new transaction to avoid detached entity issues
            Order managedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new SagaStateUpdateException(order.getOrderNumber(),
                            "Order not found"));

            managedOrder.setStatus(OrderStatus.FAILED);
            managedOrder.setUpdatedAt(LocalDateTime.now());

            SagaState sagaState = getSagaState(managedOrder);
            sagaState.setStatus(SagaStatus.FAILED);
            sagaState.setErrorMessage(exception.getMessage());

            sagaStateRepository.save(sagaState);
            orderRepository.save(managedOrder);

            log.info("Successfully marked order and saga as failed for: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to mark order and saga as failed for: {}", order.getOrderNumber(), e);
            throw new SagaStateUpdateException(order.getOrderNumber(),
                    "Failed to mark order and saga as failed", e);
        }
    }

    /**
     * Individual update methods for backward compatibility and compensation
     * scenarios.
     * These maintain individual transactions for partial recovery.
     */

    /**
     * Updates only the inventory state.
     * Used primarily for compensation scenarios.
     * Uses REQUIRES_NEW to ensure this update commits independently.
     *
     * @param order         The order being processed
     * @param transactionId The inventory transaction ID
     * @throws SagaStateUpdateException if the update fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateInventoryState(Order order, String transactionId) {
        try {
            log.debug("Updating inventory state only for order: {}", order.getOrderNumber());

            // Reload order in this new transaction to avoid detached entity issues
            Order managedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new SagaStateUpdateException(order.getOrderNumber(),
                            "Order not found"));

            SagaState sagaState = getSagaState(managedOrder);
            sagaState.setInventoryReserved(true);
            sagaState.setInventoryTransactionId(transactionId);
            sagaStateRepository.save(sagaState);

        } catch (Exception e) {
            log.error("Failed to update inventory state for order: {}", order.getOrderNumber(), e);
            throw new SagaStateUpdateException(order.getOrderNumber(),
                    "Failed to update inventory state", e);
        }
    }

    /**
     * Updates only the payment state.
     * Used primarily for compensation scenarios.
     * Uses REQUIRES_NEW to ensure this update commits independently.
     *
     * @param order         The order being processed
     * @param transactionId The payment transaction ID
     * @throws SagaStateUpdateException if the update fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePaymentState(Order order, String transactionId) {
        try {
            log.debug("Updating payment state only for order: {}", order.getOrderNumber());

            // Reload order in this new transaction to avoid detached entity issues
            Order managedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new SagaStateUpdateException(order.getOrderNumber(),
                            "Order not found"));

            SagaState sagaState = getSagaState(managedOrder);
            sagaState.setPaymentProcessed(true);
            sagaState.setPaymentTransactionId(transactionId);
            sagaStateRepository.save(sagaState);

        } catch (Exception e) {
            log.error("Failed to update payment state for order: {}", order.getOrderNumber(), e);
            throw new SagaStateUpdateException(order.getOrderNumber(),
                    "Failed to update payment state", e);
        }
    }

    /**
     * Updates only the shipping state.
     * Used primarily for compensation scenarios.
     * Uses REQUIRES_NEW to ensure this update commits independently.
     *
     * @param order          The order being processed
     * @param trackingNumber The shipping tracking number
     * @throws SagaStateUpdateException if the update fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateShippingState(Order order, String trackingNumber) {
        try {
            log.debug("Updating shipping state only for order: {}", order.getOrderNumber());

            // Reload order in this new transaction to avoid detached entity issues
            Order managedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new SagaStateUpdateException(order.getOrderNumber(),
                            "Order not found"));

            SagaState sagaState = getSagaState(managedOrder);
            sagaState.setShippingArranged(true);
            sagaState.setShippingTransactionId(trackingNumber);
            sagaStateRepository.save(sagaState);

        } catch (Exception e) {
            log.error("Failed to update shipping state for order: {}", order.getOrderNumber(), e);
            throw new SagaStateUpdateException(order.getOrderNumber(),
                    "Failed to update shipping state", e);
        }
    }

    /**
     * Updates only the saga step.
     * Used primarily for compensation scenarios.
     * Uses REQUIRES_NEW to ensure this update commits independently.
     *
     * @param order    The order being processed
     * @param nextStep The next step to transition to
     * @throws SagaStateUpdateException if the update fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSagaStep(Order order, SagaStep nextStep) {
        try {
            log.debug("Updating saga step only for order: {} to: {}", order.getOrderNumber(), nextStep);

            // Reload order in this new transaction to avoid detached entity issues
            Order managedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new SagaStateUpdateException(order.getOrderNumber(),
                            "Order not found"));

            SagaState sagaState = getSagaState(managedOrder);
            sagaState.setCurrentStep(nextStep);
            sagaState.setStatus(SagaStatus.IN_PROGRESS);
            sagaStateRepository.save(sagaState);

        } catch (Exception e) {
            log.error("Failed to update saga step for order: {}", order.getOrderNumber(), e);
            throw new SagaStateUpdateException(order.getOrderNumber(),
                    "Failed to update saga step", e);
        }
    }

    /**
     * Retrieves the saga state for an order.
     *
     * @param order The order
     * @return The saga state
     * @throws SagaStateUpdateException if saga state is not found
     */
    private SagaState getSagaState(Order order) {
        return sagaStateRepository.findByOrder(order)
                .orElseThrow(() -> new SagaStateUpdateException(order.getOrderNumber(),
                        "Saga state not found"));
    }
}
