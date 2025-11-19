package com.onlineshop.order.model;

public enum SagaStep {
    ORDER_CREATED,
    INVENTORY_VALIDATION,
    PAYMENT_PROCESSING,
    SHIPPING_ARRANGEMENT,
    ORDER_CONFIRMATION,
    COMPLETED
}
