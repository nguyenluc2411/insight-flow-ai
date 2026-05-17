package com.insightflow.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {
    private UUID id;
    private UUID tenantId;
    private String skuRoot;
    private String name;
    private String description;
    private UUID categoryId;
    private String brand;
    private String season;
    private String gender;
    private List<String> tags;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
