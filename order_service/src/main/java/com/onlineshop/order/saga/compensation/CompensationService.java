package com.onlineshop.order.saga.compensation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.model.SagaStatus;
import com.onlineshop.order.repository.SagaStateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompensationService {

    private final CompensationHandler compensationHandler;
    private final SagaStateRepository sagaStateRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateAfterFailure(Order order) {
        log.info("Starting compensation in new transaction for order: {}", order.getOrderNumber());

        SagaState sagaState = sagaStateRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Saga state not found for order: " + order.getOrderNumber()));

        try {
            compensationHandler.executeCompensation(order);
            sagaState.setStatus(SagaStatus.COMPENSATED);
            sagaStateRepository.save(sagaState);
            log.info("Compensation completed for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Compensation failed for order: {}", order.getOrderNumber(), e);
            sagaState.setStatus(SagaStatus.COMPENSATION_FAILED);
            sagaState.setErrorMessage("Compensation failed: " + e.getMessage());
            sagaStateRepository.save(sagaState);
        }
    }
}