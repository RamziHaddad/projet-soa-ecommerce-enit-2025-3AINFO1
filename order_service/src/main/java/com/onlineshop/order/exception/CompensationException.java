package com.onlineshop.order.exception;

public class CompensationException extends RuntimeException {
    
    public CompensationException(String message) {
        super(message);
    }
    
    public CompensationException(String message, Throwable cause) {
        super(message, cause);
    }
}
