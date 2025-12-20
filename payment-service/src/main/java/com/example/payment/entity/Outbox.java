package com.example.payment.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox")
public class Outbox extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID eventId;

    @Column(name = "payment_id")
    public UUID paymentId;

    @Column(name = "event_type")
    public String eventType;

    @Column(name = "payload", columnDefinition = "jsonb")
    public String payload; // Using String for JSONB

    @Column(name = "processed")
    public boolean processed = false;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    public Outbox() {}

    public Outbox(UUID paymentId, String eventType, String payload) {
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.payload = payload;
    }
}
