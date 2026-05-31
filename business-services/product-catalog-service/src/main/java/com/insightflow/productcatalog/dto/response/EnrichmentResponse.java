package com.insightflow.productcatalog.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class EnrichmentResponse {
    private String department;
    private String category;
    private String subCategory;
    private String targetDemographic;
    private String material;
    private String colorFamily;
    private Map<String, String> extractedAttributes; // Nhét vào cột JSON bên 8082
}