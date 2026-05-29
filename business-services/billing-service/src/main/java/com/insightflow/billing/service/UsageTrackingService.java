package com.insightflow.billing.service;

import com.insightflow.billing.dto.response.UsageStatusResponse;
import com.insightflow.billing.entity.TenantSubscription;
import com.insightflow.billing.entity.TenantUsage;
import com.insightflow.billing.repository.TenantSubscriptionRepository;
import com.insightflow.billing.repository.TenantUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageTrackingService {

    private final TenantUsageRepository usageRepository;
    private final TenantSubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public TenantUsage getTodayUsage(UUID tenantId) {
        return usageRepository.findByTenantIdAndUsageDate(tenantId, LocalDate.now())
                .orElse(TenantUsage.builder()
                        .tenantId(tenantId)
                        .usageDate(LocalDate.now())
                        .apiCallsCount(0)
                        .productExportsCount(0)
                        .reportsGeneratedCount(0)
                        .forecastsExecutedCount(0)
                        .storageUsedBytes(0L)
                        .build());
    }

    @Transactional
    public void incrementApiCalls(UUID tenantId) {
        ensureUsageRecord(tenantId);
        int updated = usageRepository.incrementApiCalls(tenantId, LocalDate.now());
        if (updated == 0) {
            log.warn("Failed to increment api_calls for tenantId={}", tenantId);
        }
    }

    @Transactional
    public void incrementProductExports(UUID tenantId) {
        ensureUsageRecord(tenantId);
        usageRepository.incrementProductExports(tenantId, LocalDate.now());
    }

    @Transactional
    public void incrementReportsGenerated(UUID tenantId) {
        ensureUsageRecord(tenantId);
        usageRepository.incrementReportsGenerated(tenantId, LocalDate.now());
    }

    @Transactional
    public void incrementForecastsExecuted(UUID tenantId) {
        ensureUsageRecord(tenantId);
        usageRepository.incrementForecastsExecuted(tenantId, LocalDate.now());
    }

    @Transactional
    public void updateStorageUsed(UUID tenantId, long deltaBytes) {
        TenantUsage usage = ensureUsageRecord(tenantId);
        long newBytes = Math.max(0, usage.getStorageUsedBytes() + deltaBytes);
        usage.setStorageUsedBytes(newBytes);
        usageRepository.save(usage);
    }

    @Transactional(readOnly = true)
    public UsageStatusResponse getUsageStatus(UUID tenantId) {
        TenantUsage usage = getTodayUsage(tenantId);

        int maxApiCallsPerDay = -1;
        int maxStorageGb = -1;
        var subOpt = subscriptionRepository.findActiveOrTrialByTenantId(tenantId);
        if (subOpt.isPresent()) {
            Map<String, Object> limits = subOpt.get().getLimitsAtSubscription();
            maxApiCallsPerDay = getIntLimit(limits, "max_api_calls_per_day");
            maxStorageGb = getIntLimit(limits, "max_storage_gb");
        }

        return UsageStatusResponse.builder()
                .tenantId(tenantId)
                .usageDate(usage.getUsageDate())
                .apiCallsCount(usage.getApiCallsCount())
                .maxApiCallsPerDay(maxApiCallsPerDay)
                .productExportsCount(usage.getProductExportsCount())
                .reportsGeneratedCount(usage.getReportsGeneratedCount())
                .forecastsExecutedCount(usage.getForecastsExecutedCount())
                .storageUsedBytes(usage.getStorageUsedBytes())
                .maxStorageGb(maxStorageGb)
                .build();
    }

    private TenantUsage ensureUsageRecord(UUID tenantId) {
        return usageRepository.findByTenantIdAndUsageDate(tenantId, LocalDate.now())
                .orElseGet(() -> usageRepository.save(TenantUsage.builder()
                        .tenantId(tenantId)
                        .usageDate(LocalDate.now())
                        .apiCallsCount(0)
                        .productExportsCount(0)
                        .reportsGeneratedCount(0)
                        .forecastsExecutedCount(0)
                        .storageUsedBytes(0L)
                        .build()));
    }

    private int getIntLimit(Map<String, Object> limits, String key) {
        if (limits == null || !limits.containsKey(key)) return -1;
        Object val = limits.get(key);
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
