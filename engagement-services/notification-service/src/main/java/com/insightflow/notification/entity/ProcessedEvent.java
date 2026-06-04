package com.insightflow.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "processed_events",
        schema = "notification_db",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_processed_event_event_id", columnNames = "event_id")
        },
        indexes = {
                @Index(name = "idx_processed_event_type", columnList = "event_type"),
                @Index(name = "idx_processed_event_processed_at", columnList = "processed_at"),
                @Index(name = "idx_processed_event_event_id", columnList = "event_id")
        }
)
@Getter
@Setter
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "source_service", length = 100)
    private String sourceService;

    @Column(name = "payload_hash", length = 128)
    private String payloadHash;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

