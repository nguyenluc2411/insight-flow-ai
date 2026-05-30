package com.insightflow.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * Optional metadata attached to every event.
 * Consumers should not rely on these fields for business logic — they are for
 * observability, debugging, and idempotency checks only.
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventMetadata {

    /** Forwarded from {@code X-Correlation-Id} header for distributed tracing. */
    String correlationId;

    /** Name of the service that produced this event, e.g. {@code catalog-service}. */
    String sourceService;

    /** Schema version, e.g. {@code "1"}. Increment when payload structure changes. */
    String version;

    /** Arbitrary extra context — use sparingly. */
    Map<String, String> extra;
}
