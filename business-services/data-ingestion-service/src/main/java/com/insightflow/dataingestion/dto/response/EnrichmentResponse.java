package com.insightflow.dataingestion.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class EnrichmentResponse {
    private String department;
    private String category;
    private String subCategory;
    private String targetDemographic;
    private String material;
    private String colorFamily;
    private Map<String, String> extractedAttributes;
}