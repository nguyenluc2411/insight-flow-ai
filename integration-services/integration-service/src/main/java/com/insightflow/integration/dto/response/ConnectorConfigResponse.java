package com.insightflow.integration.dto.response;

import com.insightflow.integration.core.ConnectorType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ConnectorConfigResponse {

    private UUID id;
    private UUID tenantId;
    private ConnectorType connectorType;
    private String name;
    private String status;
    private Instant lastSyncAt;
    private Instant createdAt;
    private Instant updatedAt;
}
