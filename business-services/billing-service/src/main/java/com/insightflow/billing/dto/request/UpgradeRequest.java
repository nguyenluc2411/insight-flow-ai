package com.insightflow.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpgradeRequest {

    @NotNull(message = "planId is required")
    private UUID planId;

    @NotBlank(message = "billingCycle is required")
    private String billingCycle;

    private Boolean autoRenew = true;
}
