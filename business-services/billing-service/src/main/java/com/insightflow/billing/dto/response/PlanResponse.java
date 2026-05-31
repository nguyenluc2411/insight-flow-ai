package com.insightflow.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PlanResponse {

    private UUID id;
    private UUID packageId;
    private String billingCycle;
    private Integer priceVnd;
    private String currency;
    private Integer trialDays;
    private String status;
}
