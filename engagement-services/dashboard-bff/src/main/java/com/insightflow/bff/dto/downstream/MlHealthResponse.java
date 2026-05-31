package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Health response from ml-service GET /api/v1/ml/health */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlHealthResponse {

    private String status;

    @JsonProperty("kafkaConnected")
    private Boolean kafkaConnected;

    @JsonProperty("modelsLoaded")
    private Integer modelsLoaded;

    @JsonProperty("dbConnected")
    private Boolean dbConnected;
}
