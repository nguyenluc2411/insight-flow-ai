package com.insightflow.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "processed_events", schema = "notification_db")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt = Instant.now();

    public ProcessedEvent(String eventId, String topic) {
        this.eventId = eventId;
        this.topic = topic;
    }
}
