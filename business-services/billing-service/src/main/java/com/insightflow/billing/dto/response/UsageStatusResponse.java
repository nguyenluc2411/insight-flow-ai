package com.insightflow.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UsageStatusResponse {

    private UUID tenantId;
    private LocalDate usageDate;
    private Integer apiCallsCount;
    private Integer maxApiCallsPerDay;
    private Integer productExportsCount;
    private Integer reportsGeneratedCount;
    private Integer forecastsExecutedCount;
    private Long storageUsedBytes;
    private Integer maxStorageGb;
}
