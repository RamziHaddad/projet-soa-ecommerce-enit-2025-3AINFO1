package com.onlineshop.order.saga;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.model.SagaStatus;
import com.onlineshop.order.model.SagaStep;
import com.onlineshop.order.repository.SagaStateRepository;
import com.onlineshop.order.saga.compensation.CompensationService;
import com.onlineshop.order.saga.retry.RetryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of SAGA orchestration pattern with proper transaction
 * boundaries.
 * Refactored to use async step execution with REQUIRES_NEW propagation,
 * ensuring
 * each saga step commits independently before the next begins.
 * 
 * Key improvements:
 * - Removed single transaction spanning all steps (breaking SAGA pattern)
 * - Each step now executes asynchronously with its own transaction
 * - State updates commit before proceeding to next step
 * - Compensation can access committed state from completed steps
 * - Retry mechanism works correctly with committed state
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorImpl implements SagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final CompensationService compensationService;
    private final RetryService retryService;
    private final SagaStepExecutor sagaStepExecutor;

    @Override
    public void startSaga(Order order) {
        log.info("Starting SAGA for order: {}", order.getOrderNumber());

        createInitialSagaState(order);

        try {
            sagaStepExecutor.executeInventoryStep(order.getId());
            log.info("SAGA workflow initiated for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to initiate SAGA for order: {}", order.getOrderNumber(), e);
        }
    }

    /**
     * Creates initial saga state in a new transaction that commits immediately.
     * This ensures the saga state is persisted before any steps execute.
     *
     * @param order The order to create saga state for
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void createInitialSagaState(Order order) {
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
        log.debug("Initial saga state created for order: {}", order.getOrderNumber());
    }

    @Override
    public void executeNextStep(Order order) {
        SagaState sagaState = getSagaState(order);
        log.info("Executing SAGA step: {} for order: {}", sagaState.getCurrentStep(), order.getOrderNumber());

        switch (sagaState.getCurrentStep()) {
            case ORDER_CREATED, INVENTORY_VALIDATION:
                sagaStepExecutor.executeInventoryStep(order.getId());
                break;
            case PAYMENT_PROCESSING:
                sagaStepExecutor.executePaymentStep(order.getId());
                break;
            case SHIPPING_ARRANGEMENT:
                sagaStepExecutor.executeShippingStep(order.getId());
                break;
            case ORDER_CONFIRMATION, COMPLETED:
                sagaStepExecutor.completeOrder(order.getId());
                break;
            default:
                throw new IllegalStateException("Unknown SAGA step: " + sagaState.getCurrentStep());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(Order order) {
        compensationService.compensateAfterFailure(order);
    }

    @Override
    public boolean canRetry(Order order) {
        SagaState sagaState = getSagaState(order);
        return retryService.canRetry(sagaState) && sagaState.getRetryable();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

            // Execute next step asynchronously
            executeNextStep(order);
            log.info("SAGA retry initiated successfully for order: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("SAGA retry failed for order: {}", order.getOrderNumber(), e);
            retryService.markRetryFailed(sagaState, e);
            throw e;
        }
    }

    private SagaState getSagaState(Order order) {
        return sagaStateRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Saga state not found for order: " + order.getOrderNumber()));
    }
}
