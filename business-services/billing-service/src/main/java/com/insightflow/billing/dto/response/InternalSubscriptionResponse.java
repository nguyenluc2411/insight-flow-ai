package com.insightflow.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class InternalSubscriptionResponse {

    private UUID subscriptionId;
    private UUID tenantId;
    private UUID planId;
    private String status;
    private List<String> featureCodes;
    private Map<String, Object> limits;
    private Integer planVersion;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean autoRenew;
}
