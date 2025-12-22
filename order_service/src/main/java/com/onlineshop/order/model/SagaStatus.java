package com.onlineshop.order.model;

public enum SagaStatus {
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    RETRYING,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED,
    FAILED
}
