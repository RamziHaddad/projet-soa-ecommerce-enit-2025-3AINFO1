package com.onlineshop.order.exception;

public class ShippingArrangementException extends RuntimeException {

    public ShippingArrangementException(String message) {
        super(message);
    }

    public ShippingArrangementException(String message, Throwable cause) {
        super(message, cause);
    }
}
