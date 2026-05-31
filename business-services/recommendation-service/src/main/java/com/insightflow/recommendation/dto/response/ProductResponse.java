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
public class ProductResponse {
    private String id;
    private String productCode;
    private String productName;
    private String brand;
    private String department;
    private String category;
    private String subCategory;
    private String targetDemographic;
    private String material;
    private String fitType;
    private String pattern;
    private String styleContext;
    private String season;
    private String attributes;

    // Kế thừa từ BaseEntity bên 8082
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}