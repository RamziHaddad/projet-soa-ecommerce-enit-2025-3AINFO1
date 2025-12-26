package com.enit.catalog.service;

import com.enit.catalog.entity.OutBoxEvent;
import com.enit.catalog.entity.Product;
import com.enit.catalog.entity.enums.EventType;

//Service de gestion des événements Outbox.
public interface OutBoxEventService {

    OutBoxEvent createProductCreatedEvent(Product product);

    OutBoxEvent createProductUpdatedEvent(Product product);

    OutBoxEvent createProductDeletedEvent(Long productId);

    void markAsSucceeded(Long eventId);

    void markAsFailed(Long eventId, String errorMessage);

    void markAsDeadLetter(Long eventId, String errorMessage);

    OutBoxEvent createEvent(String payload, EventType eventType);
}
