package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** Paged response from ml-service GET /api/v1/ml/recommendations */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlPagedRecommendationsResponse {

    private List<MlRecommendationItem> items;
    private int page;
    private int size;
    private long total;
}
