package com.enit.catalog.entity;

import java.time.LocalDateTime;

import com.enit.catalog.entity.enums.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inbox_events")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId; // Identifiant unique pour la déduplication

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isProcessed = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isProcessed == null) {
            isProcessed = false;
        }
    }

    public void markAsProcessed() {
        this.isProcessed = true;
        this.processedAt = LocalDateTime.now();
    }
}

