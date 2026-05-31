package com.insightflow.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {
    private String id;
    private String productId;
    private String sku;
    private String colorFamily;
    private String colorName;
    private String size;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}