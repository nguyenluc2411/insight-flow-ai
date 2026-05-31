package com.insightflow.recommendation.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Cực kỳ quan trọng để bỏ qua field lạ, chống sập
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