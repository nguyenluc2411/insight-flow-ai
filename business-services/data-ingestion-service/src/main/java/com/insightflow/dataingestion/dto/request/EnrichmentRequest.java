package com.insightflow.dataingestion.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnrichmentRequest {
    private String productName;
    private String rawCategory;
    private String rawColor;
}