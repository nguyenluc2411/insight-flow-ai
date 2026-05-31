package com.insightflow.billing.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DowngradeRequest {

    @NotNull(message = "planId is required")
    private UUID planId;

    private String reason;
}
