package com.insightflow.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class EntitlementResponse {

    private UUID tenantId;
    private String subscriptionStatus;
    private List<String> featureCodes;
    private boolean cached;
}
