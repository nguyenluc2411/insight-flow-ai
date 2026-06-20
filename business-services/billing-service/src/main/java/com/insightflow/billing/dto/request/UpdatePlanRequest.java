package com.insightflow.billing.dto.request;

import lombok.Data;

/** Super-admin: patch a plan's pricing. Null fields are left unchanged. */
@Data
public class UpdatePlanRequest {
    private Integer priceVnd;
    private Integer trialDays;
    private String billingCycle;
    private String status;
}
