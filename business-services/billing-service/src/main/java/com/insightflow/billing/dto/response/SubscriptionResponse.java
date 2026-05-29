package com.insightflow.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class SubscriptionResponse {

    private UUID id;
    private UUID tenantId;
    private UUID planId;
    private Integer priceAtSubscription;
    private List<String> featuresAtSubscription;
    private Map<String, Object> limitsAtSubscription;
    private Integer planVersion;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean autoRenew;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
