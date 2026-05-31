package com.insightflow.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUpgradeRequestRequest {

    @NotBlank(message = "packageCode is required")
    private String packageCode;

    /** Optional — defaults to MONTHLY. */
    private String billingCycle;
}
