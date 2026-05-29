package com.insightflow.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UpgradeRequestResponse {

    private UUID id;
    private UUID tenantId;
    private String requestedPackageCode;
    private String billingCycle;
    private String status;
    private String note;
    private Instant resolvedAt;
    private Instant createdAt;
}
