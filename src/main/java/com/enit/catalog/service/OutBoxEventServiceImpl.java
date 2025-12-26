package com.enit.catalog.service;

import com.enit.catalog.entity.OutBoxEvent;
import com.enit.catalog.entity.Product;
import com.enit.catalog.entity.enums.EventType;
import com.enit.catalog.entity.enums.Status;
import com.enit.catalog.mapper.OutboxPayloadMapper;
import com.enit.catalog.repository.OutBoxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutBoxEventServiceImpl implements OutBoxEventService {

    private final OutBoxEventRepository outBoxEventRepository;
    private final OutboxPayloadMapper payloadMapper;

    @Override
    @Transactional
    public OutBoxEvent createProductCreatedEvent(Product product) {
        String payload = payloadMapper.toCreateUpdatePayload(product);
        return createEvent(payload, EventType.CREATE);
    }

    @Override
    @Transactional
    public OutBoxEvent createProductUpdatedEvent(Product product) {
        String payload = payloadMapper.toCreateUpdatePayload(product);
        return createEvent(payload, EventType.UPDATE);
    }

    @Override
    @Transactional
    public OutBoxEvent createProductDeletedEvent(Long productId) {
        String payload = payloadMapper.toDeletePayload(productId);
        return createEvent(payload, EventType.DELETE);
    }

    @Override
    @Transactional
    public OutBoxEvent createEvent(String payload, EventType eventType) {
        OutBoxEvent event = OutBoxEvent.builder()
                .payload(payload)
                .status(Status.PENDING)
                .eventType(eventType)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(5)
                .build();

        return outBoxEventRepository.save(event);
    }

    @Override
    @Transactional
    public void markAsSucceeded(Long eventId) {
        outBoxEventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(Status.SUCCEEDED);
            event.setProcessedAt(LocalDateTime.now());
            outBoxEventRepository.save(event);
        });
    }

    @Override
    @Transactional
    public void markAsFailed(Long eventId, String errorMessage) {
        outBoxEventRepository.findById(eventId).ifPresent(event -> {
            event.incrementRetryCount();
            event.setErrorMessage(errorMessage);
            event.setStatus(event.canRetry() ? Status.FAILED : Status.DEAD_LETTER);
            event.setProcessedAt(LocalDateTime.now());
            outBoxEventRepository.save(event);
        });
    }

    @Override
    @Transactional
    public void markAsDeadLetter(Long eventId, String errorMessage) {
        outBoxEventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(Status.DEAD_LETTER);
            event.setErrorMessage(errorMessage);
            event.setProcessedAt(LocalDateTime.now());
            outBoxEventRepository.save(event);
        });
    }
}
