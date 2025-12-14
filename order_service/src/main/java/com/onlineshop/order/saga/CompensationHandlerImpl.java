package com.onlineshop.order.saga;

import org.springframework.stereotype.Component;

import com.onlineshop.order.communication.OrderProcessingCommunicationHandler;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.repository.SagaStateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of compensation handling
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompensationHandlerImpl implements CompensationHandler {

    private final OrderProcessingCommunicationHandler communicationStrategy;
    private final SagaStateRepository sagaStateRepository;

    @Override
    public void compensateInventory(Order order) {
        log.info("Compensating inventory for order: {}", order.getOrderNumber());

        try {
            SagaState sagaState = getSagaState(order);
            if (Boolean.TRUE.equals(sagaState.getInventoryReserved()) &&
                    sagaState.getInventoryTransactionId() != null) {

                // Release inventory using transaction ID
                var releaseResponse = communicationStrategy.releaseInventory(
                        sagaState.getInventoryTransactionId());

                if (releaseResponse != null && Boolean.TRUE.equals(releaseResponse.getSuccess())) {
                    sagaState.setInventoryReserved(false);
                    sagaState.setInventoryTransactionId(null);
                    sagaStateRepository.save(sagaState);
                    log.info("Inventory released successfully for order: {}", order.getOrderNumber());
                } else {
                    log.warn("Failed to release inventory for order: {}", order.getOrderNumber());
                }
            }
        } catch (Exception e) {
            log.error("Error compensating inventory for order: {}", order.getOrderNumber(), e);
        }
    }

    @Override
    public void compensatePayment(Order order) {
        log.info("Compensating payment for order: {}", order.getOrderNumber());

        try {
            SagaState sagaState = getSagaState(order);
            if (Boolean.TRUE.equals(sagaState.getPaymentProcessed()) &&
                    sagaState.getPaymentTransactionId() != null) {

                // Refund payment using transaction ID
                var refundResponse = communicationStrategy.refundPayment(
                        sagaState.getPaymentTransactionId());

                if (refundResponse != null && Boolean.TRUE.equals(refundResponse.getSuccess())) {
                    sagaState.setPaymentProcessed(false);
                    sagaState.setPaymentTransactionId(null);
                    sagaStateRepository.save(sagaState);
                    log.info("Payment refunded successfully for order: {}", order.getOrderNumber());
                } else {
                    log.warn("Failed to refund payment for order: {}", order.getOrderNumber());
                }
            }
        } catch (Exception e) {
            log.error("Error compensating payment for order: {}", order.getOrderNumber(), e);
        }
    }

    @Override
    public void compensateShipping(Order order) {
        log.info("Compensating shipping for order: {}", order.getOrderNumber());

        try {
            SagaState sagaState = getSagaState(order);
            if (Boolean.TRUE.equals(sagaState.getShippingArranged()) &&
                    sagaState.getShippingTransactionId() != null) {

                // Cancel shipping using tracking number
                var cancelResponse = communicationStrategy.cancelShipping(
                        sagaState.getShippingTransactionId());

                if (cancelResponse != null && Boolean.TRUE.equals(cancelResponse.getSuccess())) {
                    sagaState.setShippingArranged(false);
                    sagaState.setShippingTransactionId(null);
                    sagaStateRepository.save(sagaState);
                    log.info("Shipping cancelled successfully for order: {}", order.getOrderNumber());
                } else {
                    log.warn("Failed to cancel shipping for order: {}", order.getOrderNumber());
                }
            }
        } catch (Exception e) {
            log.error("Error compensating shipping for order: {}", order.getOrderNumber(), e);
        }
    }

    @Override
    public void executeCompensation(Order order) {
        log.info("Executing full compensation for order: {}", order.getOrderNumber());

        SagaState sagaState = getSagaState(order);

        // Execute compensation in reverse order of execution
        try {
            // 1. Cancel shipping first (if it was arranged)
            if (Boolean.TRUE.equals(sagaState.getShippingArranged())) {
                compensateShipping(order);
            }

            // 2. Refund payment (if it was processed)
            if (Boolean.TRUE.equals(sagaState.getPaymentProcessed())) {
                compensatePayment(order);
            }

            // 3. Release inventory (if it was reserved)
            if (Boolean.TRUE.equals(sagaState.getInventoryReserved())) {
                compensateInventory(order);
            }

            log.info("Compensation completed for order: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Error during compensation for order: {}", order.getOrderNumber(), e);
            // Even if compensation fails, we continue to ensure all steps are attempted
        }
    }

    private SagaState getSagaState(Order order) {
        return sagaStateRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Saga state not found for order: " + order.getOrderNumber()));
    }
}
