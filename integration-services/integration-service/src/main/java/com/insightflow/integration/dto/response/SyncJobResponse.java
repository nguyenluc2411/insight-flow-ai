package com.insightflow.integration.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SyncJobResponse {

    private UUID id;
    private UUID tenantId;
    private UUID connectorConfigId;
    private String entityType;
    private String syncType;
    private String status;
    private int recordsProcessed;
    private int recordsFailed;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
}
