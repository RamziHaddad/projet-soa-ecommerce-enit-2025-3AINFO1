package com.enit.catalog.scheduler;

import com.enit.catalog.communication.CommunicationService;
import com.enit.catalog.dto.request.RequestSearch;
import com.enit.catalog.entity.OutBoxEvent;
import com.enit.catalog.entity.enums.EventType;
import com.enit.catalog.entity.enums.Status;
import com.enit.catalog.mapper.OutboxPayloadMapper;
import com.enit.catalog.repository.OutBoxEventRepository;
import com.enit.catalog.service.OutBoxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchProviderImpl implements SearchProvider {

    private final OutBoxEventRepository outBoxEventRepository;
    private final OutBoxEventService outBoxEventService;
    private final CommunicationService communicationService;
    private final OutboxPayloadMapper payloadMapper;

    @Value("${outbox.batch.size:100}")
    private int batchSize;

    @Value("${outbox.stuck.threshold.minutes:5}")
    private int stuckThresholdMinutes;

    @Override
    @Scheduled(fixedDelayString = "${outbox.scheduler.delay:30000}")
    @Transactional
    public void processOutboxEvents() {
        recoverStuckEvents();

        List<OutBoxEvent> pendingEvents = outBoxEventRepository
                .findByStatusWithLimit(Status.PENDING, PageRequest.of(0, batchSize));

        if (pendingEvents.isEmpty()) {
            return;
        }

        // Grouper les événements par type (CREATE, UPDATE, DELETE)
        Map<EventType, List<OutBoxEvent>> eventsByType = pendingEvents.stream()
                .collect(Collectors.groupingBy(OutBoxEvent::getEventType));

        // Traiter chaque groupe en batch
        eventsByType.forEach(this::processBatchByType);
    }

    private void processBatchByType(EventType eventType, List<OutBoxEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        List<Long> eventIds = new ArrayList<>();
        List<RequestSearch> requests = new ArrayList<>();

        for (OutBoxEvent event : events) {
            try {
                event.setStatus(Status.PROCESSING);
                event.setProcessedAt(LocalDateTime.now());
                outBoxEventRepository.save(event);

                // Utiliser le mapper pour convertir le payload
                boolean includeDetails = (eventType != EventType.DELETE);
                RequestSearch request = payloadMapper.toRequestSearch(event.getPayload(), includeDetails);
                
                requests.add(request);
                eventIds.add(event.getId());
            } catch (Exception e) {
                outBoxEventService.markAsFailed(event.getId(), "Erreur parsing: " + e.getMessage());
            }
        }

        if (requests.isEmpty()) {
            return;
        }

        // Envoyer le batch au Search Service
        try {
            switch (eventType) {
                case CREATE -> communicationService.syncCreateBatch(requests);
                case UPDATE -> communicationService.syncUpdateBatch(requests);
                case DELETE -> communicationService.syncDeleteBatch(requests);
            }

            // Succès - Marquer tous les événements comme SUCCEEDED
            eventIds.forEach(outBoxEventService::markAsSucceeded);

        } catch (Exception e) {
            eventIds.forEach(id -> outBoxEventService.markAsFailed(id, "Erreur batch: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public void recoverStuckEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stuckThresholdMinutes);
        int resetCount = outBoxEventRepository.resetStuckEvents(threshold);

        if (resetCount > 0) {
            // log.info("Réinitialisation de {} événements bloqués", resetCount);
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${outbox.retry.delay:10000}")
    @Transactional
    public void retryFailedEvents() {
        List<OutBoxEvent> failedEvents = outBoxEventRepository
                .findRetryableEvents(PageRequest.of(0, batchSize));

        if (failedEvents.isEmpty()) {
            return;
        }

        for (OutBoxEvent event : failedEvents) {
            event.setStatus(Status.PENDING);
            outBoxEventRepository.save(event);
        }
    }
}
