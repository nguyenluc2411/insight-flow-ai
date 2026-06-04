package com.insightflow.dataingestion.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope<T> {
    @JsonProperty("event_id")
    private String eventId;
    @JsonProperty("event_type")
    private String eventType;
    @JsonProperty("timestamp")
    private String timestamp;
    @JsonProperty("source")
    private String source;
    @JsonProperty("payload")
    private T payload;
}