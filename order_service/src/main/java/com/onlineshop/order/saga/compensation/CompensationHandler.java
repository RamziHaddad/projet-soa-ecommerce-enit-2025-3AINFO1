package com.onlineshop.order.saga.compensation;

import com.onlineshop.order.model.Order;

/**
 * Interface for handling compensating transactions
 */
public interface CompensationHandler {
    
    /**
     * Compensate inventory reservation
     * @param order The order to compensate
     */
    void compensateInventory(Order order);
    
    /**
     * Compensate payment processing
     * @param order The order to compensate
     */
    void compensatePayment(Order order);
    
    /**
     * Compensate shipping arrangement
     * @param order The order to compensate
     */
    void compensateShipping(Order order);
    
    /**
     * Execute full compensation workflow
     * @param order The order to compensate
     */
    void executeCompensation(Order order);
}
