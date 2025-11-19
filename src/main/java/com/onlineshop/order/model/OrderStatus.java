package com.onlineshop.order.model;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSED,
    SHIPPING_ARRANGED,
    COMPLETED,
    FAILED,
    CANCELLED,
    COMPENSATING
}
