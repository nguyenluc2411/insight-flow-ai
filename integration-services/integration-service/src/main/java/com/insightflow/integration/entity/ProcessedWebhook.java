package com.insightflow.integration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_webhooks", schema = "integration_db")
@Getter
@Setter
public class ProcessedWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "connector_type", nullable = false, length = 50)
    private String connectorType;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "external_event_id", nullable = false, length = 255)
    private String externalEventId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "signature", length = 255)
    private String signature;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt = Instant.now();
}
