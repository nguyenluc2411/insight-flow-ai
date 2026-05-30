package com.insightflow.common.events.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.insightflow.common.events.EventMetadata;
import com.insightflow.common.events.EventTopics;
import com.insightflow.common.events.InsightFlowEvent;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by {@code integration-service} after a POS sync job finishes (success or partial error).
 * Consumers: {@code catalog-service}, {@code sales-service}
 * Topic: {@value EventTopics#INTEGRATION_SYNC_COMPLETED}
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntegrationSyncCompletedEvent implements InsightFlowEvent {

    @Builder.Default
    String eventId = UUID.randomUUID().toString();

    @Builder.Default
    String eventType = EventTopics.INTEGRATION_SYNC_COMPLETED;

    UUID tenantId;

    @Builder.Default
    Instant occurredAt = Instant.now();

    EventMetadata metadata;

    // --- Domain fields ---

    UUID syncJobId;

    /** POS source: KIOTVIET, SAPO, HARAVAN */
    String source;

    /** Sync strategy: FULL, INCREMENTAL, WEBHOOK */
    String syncType;

    int productsSynced;
    int ordersSynced;

    Instant syncStartedAt;
    Instant syncCompletedAt;

    boolean hasErrors;

    /** Short human-readable summary of errors, null when hasErrors is false. */
    String errorSummary;
}
