package com.insightflow.notification.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

/** Payload for ml.forecast.generated events published by ml-service. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlForecastPayload {
    private String eventId;
    private String eventType;
    @JsonProperty("tenantId")
    private UUID tenantId;
    @JsonProperty("variantId")
    private UUID variantId;
    private String confidence; // high, medium, low
    @JsonProperty("forecastDays")
    private int forecastDays;
}
