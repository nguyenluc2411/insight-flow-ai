package com.insightflow.billing.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UsageCheckRequest {

    @NotNull(message = "tenantId is required")
    private UUID tenantId;

    private String featureCode;
}
