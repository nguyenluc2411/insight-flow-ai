package com.insightflow.common.events.catalog;

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
 * Published by {@code catalog-service} when a new product is created.
 * Consumers: {@code ml-service}, {@code dashboard-bff}
 * Topic: {@value EventTopics#CATALOG_PRODUCT_CREATED}
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CatalogProductCreatedEvent implements InsightFlowEvent {

    @Builder.Default
    String eventId = UUID.randomUUID().toString();

    @Builder.Default
    String eventType = EventTopics.CATALOG_PRODUCT_CREATED;

    UUID tenantId;

    @Builder.Default
    Instant occurredAt = Instant.now();

    EventMetadata metadata;

    // --- Domain fields ---

    UUID productId;
    String name;
    String sku;

    /** Category slug, e.g. {@code "ao-thun"} */
    String category;

    /** Product status at creation: ACTIVE, INACTIVE, DRAFT */
    String status;
}
