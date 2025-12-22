package com.onlineshop.order.saga.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.model.SagaStatus;
import com.onlineshop.order.repository.SagaStateRepository;
import com.onlineshop.order.saga.SagaOrchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaRetryScheduler {

    private final SagaStateRepository sagaStateRepository;
    private final SagaOrchestrator sagaOrchestrator;

    /**
     * Process sagas ready for retry (every 30 seconds)
     */
    @Scheduled(fixedRate = 30000)
    public void processReadyRetries() {
        log.debug("Checking for sagas ready for retry...");

        LocalDateTime now = LocalDateTime.now();
        List<SagaState> candidates = sagaStateRepository.findReadyForRetry(SagaStatus.IN_PROGRESS, now);

        log.debug("Found {} candidate sagas for retry.", candidates.size());

        for (SagaState candidate : candidates) {

            int updated = sagaStateRepository.markAsRetrying(
                    candidate.getId(),
                    SagaStatus.RETRYING,
                    SagaStatus.IN_PROGRESS);

            if (updated > 0) {

                try {
                    Order order = candidate.getOrder();
                    log.info("Processing retry for order: {}", order.getOrderNumber());

                    if (sagaOrchestrator.canRetry(order)) {
                        sagaOrchestrator.retrySaga(order);
                    } else {
                        log.warn("Order {} cannot be retried - will remain in RETRYING state", order.getOrderNumber());

                    }

                } catch (Exception e) {
                    log.error("Unexpected error during retry of order: {}",
                            candidate.getOrder().getOrderNumber(), e);

                }
            }

        }

        log.debug("Retry processing completed.");
    }

    /**
     * Detect and recover stuck sagas (every 5 minutes)
     */
    @Scheduled(fixedRate = 300000)
    public void checkStuckSagas() {
        log.debug("Checking for stuck sagas...");

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        List<SagaState> stuckSagas = sagaStateRepository.findStuckSagas(SagaStatus.FAILED, cutoff);

        log.debug("Found {} potentially stuck sagas.", stuckSagas.size());

        for (SagaState sagaState : stuckSagas) {

            int updated = sagaStateRepository.markAsRetrying(
                    sagaState.getId(),
                    SagaStatus.RETRYING,
                    SagaStatus.FAILED);

            if (updated > 0) {
                try {
                    Order order = sagaState.getOrder();
                    log.info("Initiating retry for stuck saga: {}", order.getOrderNumber());

                    if (sagaOrchestrator.canRetry(order)) {
                        sagaOrchestrator.retrySaga(order);
                    }

                } catch (Exception e) {
                    log.error("Error retrying stuck saga for order: {}",
                            sagaState.getOrder().getOrderNumber(), e);
                }
            }
        }

        log.debug("Stuck saga check completed.");
    }
}