package com.enit.catalog.repository;

import com.enit.catalog.entity.InboxEvent;
import com.enit.catalog.entity.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {
    boolean existsByEventId(String eventId);

    Optional<InboxEvent> findByEventId(String eventId);

    Optional<InboxEvent> findByEventIdAndEventType(String eventId, EventType eventType);

    boolean existsByEventIdAndIsProcessedTrue(String eventId);
}
