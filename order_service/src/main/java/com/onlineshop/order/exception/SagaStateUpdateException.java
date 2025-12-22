package com.onlineshop.order.exception;

/**
 * Exception thrown when a saga state update operation fails.
 * This exception provides context about the order and the specific error that
 * occurred.
 */
public class SagaStateUpdateException extends RuntimeException {

    private final String orderNumber;

    public SagaStateUpdateException(String orderNumber, String message) {
        super(String.format("Failed to update saga state for order %s: %s", orderNumber, message));
        this.orderNumber = orderNumber;
    }

    public SagaStateUpdateException(String orderNumber, String message, Throwable cause) {
        super(String.format("Failed to update saga state for order %s: %s", orderNumber, message), cause);
        this.orderNumber = orderNumber;
    }

    public String getOrderNumber() {
        return orderNumber;
    }
}
