package com.insightflow.userworkspace.dto.event;

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
    private String timestamp;
    private String source;
    private T payload;
}