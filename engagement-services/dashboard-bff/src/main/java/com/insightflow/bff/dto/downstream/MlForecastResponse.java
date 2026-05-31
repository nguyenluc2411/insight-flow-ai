package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Single variant forecast from ml-service */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlForecastResponse {

    @JsonProperty("variantId")
    private UUID variantId;

    @JsonProperty("tenantId")
    private UUID tenantId;

    @JsonProperty("forecastDays")
    private int forecastDays;

    private String confidence;
    private String basis;

    private List<ForecastPoint> predictions;

    @JsonProperty("generatedAt")
    private Instant generatedAt;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastPoint {
        private String date;
        @JsonProperty("predictedQty")
        private Double predictedQty;
    }
}
