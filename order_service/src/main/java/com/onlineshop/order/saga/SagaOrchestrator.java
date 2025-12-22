package com.onlineshop.order.saga;

import com.onlineshop.order.model.Order;

/**
 * Main interface for SAGA orchestration
 */
public interface SagaOrchestrator {
    
    /**
     * Start the SAGA workflow for an order
     * @param order The order to process
     */
    void startSaga(Order order);
    
    /**
     * Execute the next step in the SAGA
     * @param order The order being processed
     */
    void executeNextStep(Order order);
    
    /**
     * Handle compensation when a step fails
     * @param order The order that failed
     */
    void compensate(Order order);
    
    /**
     * Check if SAGA can be retried
     * @param order The order to check
     * @return true if retry is possible
     */
    boolean canRetry(Order order);

    /**
     * Retry a failed SAGA execution
     * @param order The order to retry
     */
    void retrySaga(Order order);
}
