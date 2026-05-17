package com.insightflow.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class LocationResponse {
    private UUID id;
    private UUID tenantId;
    private String name;
    private String type;
    private String address;
    private String city;
    private boolean active;
    private Instant createdAt;
}
