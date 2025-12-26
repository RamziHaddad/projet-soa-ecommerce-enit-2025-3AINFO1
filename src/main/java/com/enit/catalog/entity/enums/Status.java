package com.enit.catalog.entity.enums;

public enum Status {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    DEAD_LETTER // Événements ayant dépassé le nombre max de tentatives
}
