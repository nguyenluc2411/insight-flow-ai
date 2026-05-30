package com.insightflow.common.events.ml;

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
 * Published by {@code ml-service} after a demand forecast run completes.
 * Consumers: {@code dashboard-bff}, {@code notification-service}
 * Topic: {@value EventTopics#ML_FORECAST_GENERATED}
 *
 * <p>Phase 1 uses Prophet for forecasting. {@code modelVersion} identifies
 * the model artifact used so results can be traced back to a specific training run.
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MlForecastGeneratedEvent implements InsightFlowEvent {

    @Builder.Default
    String eventId = UUID.randomUUID().toString();

    @Builder.Default
    String eventType = EventTopics.ML_FORECAST_GENERATED;

    UUID tenantId;

    @Builder.Default
    Instant occurredAt = Instant.now();

    EventMetadata metadata;

    // --- Domain fields ---

    UUID forecastId;
    UUID productId;
    String productSku;

    int forecastHorizonDays;
    double forecastedDemand;

    /** Confidence score in [0.0, 1.0] */
    double confidenceScore;

    /** Artifact identifier, e.g. {@code "prophet-v1.2"} */
    String modelVersion;

    Instant forecastPeriodStart;
    Instant forecastPeriodEnd;
}
