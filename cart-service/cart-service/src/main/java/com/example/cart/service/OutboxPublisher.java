package com.example.cart.service;
import com.example.cart.entity.OrdersOutbox;
import com.example.cart.repository.OrdersOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OutboxPublisher {

    private final OrdersOutboxRepository outboxRepository;
    private final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    public OutboxPublisher(OrdersOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public int publishPending() {
        List<OrdersOutbox> pending = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        int publishedCount = 0;
        for (OrdersOutbox ev : pending) {
            try {
                // Simulate delivering the event to a message broker or another service.
                // Replace this with real broker code (Kafka, RabbitMQ, HTTP, etc.) in production.
                log.info("Publishing outbox event {} type={} payload={}", ev.getId(), ev.getEventType(), ev.getPayload());

                ev.setAttempts(ev.getAttempts() + 1);
                ev.setPublished(true);
                ev.setPublishedAt(OffsetDateTime.now());
                outboxRepository.save(ev);
                publishedCount++;
            } catch (Exception ex) {
                // mark attempt and continue; do not mark published
                log.error("Failed to publish outbox event {}: {}", ev.getId(), ex.getMessage());
                ev.setAttempts(ev.getAttempts() + 1);
                outboxRepository.save(ev);
            }
        }
        return publishedCount;
    }
}
