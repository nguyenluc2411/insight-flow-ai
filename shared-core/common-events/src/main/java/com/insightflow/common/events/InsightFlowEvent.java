package com.insightflow.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for all Insight Flow AI Kafka events.
 * Topic naming convention: {@code {domain}.{entity}.{action}}
 * All events carry the mandatory fields defined here.
 */
public interface InsightFlowEvent {

    String getEventId();

    String getEventType();

    UUID getTenantId();

    Instant getOccurredAt();

    EventMetadata getMetadata();
}
