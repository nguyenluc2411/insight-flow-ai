package com.insightflow.productcatalog.dto.request;

import lombok.Data;

@Data
public class EnrichmentRequest {
    private String productName;
    private String rawCategory;
    private String rawColor;
}