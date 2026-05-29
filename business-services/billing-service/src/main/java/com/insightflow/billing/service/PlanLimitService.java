package com.insightflow.billing.service;

import com.insightflow.billing.dto.response.RateLimitResponse;
import com.insightflow.billing.entity.PlanLimit;
import com.insightflow.billing.entity.TenantSubscription;
import com.insightflow.billing.entity.TenantUsage;
import com.insightflow.billing.repository.PlanLimitRepository;
import com.insightflow.billing.repository.TenantSubscriptionRepository;
import com.insightflow.billing.repository.TenantUsageRepository;
import com.insightflow.common.web.exception.ResourceNotFoundException;
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
@Transactional(readOnly = true)
public class PlanLimitService {

    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantUsageRepository usageRepository;
    private final PlanLimitRepository planLimitRepository;
    private final UsageTrackingService usageTrackingService;

    public RateLimitResponse getRateLimitResponse(UUID tenantId) {
        TenantSubscription subscription = subscriptionRepository.findActiveOrTrialByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for tenant: " + tenantId));

        // Get limits from snapshot stored at subscription time
        Map<String, Object> limitsSnapshot = subscription.getLimitsAtSubscription();
        int maxApiCallsPerDay = getIntLimit(limitsSnapshot, "max_api_calls_per_day");
        int rateLimitPerMinute = getIntLimit(limitsSnapshot, "api_rate_limit_per_minute");

        // If unlimited
        if (maxApiCallsPerDay == -1) {
            return RateLimitResponse.builder()
                    .allowed(true)
                    .remaining(-1)
                    .limit(-1)
                    .rateLimitPerMinute(rateLimitPerMinute)
                    .build();
        }

        // Get today's usage
        TenantUsage usage = usageRepository.findByTenantIdAndUsageDate(tenantId, LocalDate.now())
                .orElse(null);
        int usedToday = (usage != null && usage.getApiCallsCount() != null) ? usage.getApiCallsCount() : 0;
        int remaining = Math.max(0, maxApiCallsPerDay - usedToday);
        boolean allowed = usedToday < maxApiCallsPerDay;

        return RateLimitResponse.builder()
                .allowed(allowed)
                .remaining(remaining)
                .limit(maxApiCallsPerDay)
                .rateLimitPerMinute(rateLimitPerMinute)
                .build();
    }

    /**
     * Atomically counts one API call against the tenant's daily quota and reports
     * whether the call is allowed. Called synchronously by the gateway rate-limit
     * filter on every authenticated request.
     *
     * The over-limit request is rejected WITHOUT being counted, so the counter
     * never exceeds the quota.
     */
    @Transactional
    public RateLimitResponse checkAndConsumeApiCall(UUID tenantId) {
        TenantSubscription subscription = subscriptionRepository.findActiveOrTrialByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for tenant: " + tenantId));

        Map<String, Object> limitsSnapshot = subscription.getLimitsAtSubscription();
        int maxApiCallsPerDay = getIntLimit(limitsSnapshot, "max_api_calls_per_day");
        int rateLimitPerMinute = getIntLimit(limitsSnapshot, "api_rate_limit_per_minute");

        // Unlimited plan — still count for analytics, never reject.
        if (maxApiCallsPerDay == -1) {
            usageTrackingService.incrementApiCalls(tenantId);
            return RateLimitResponse.builder()
                    .allowed(true).remaining(-1).limit(-1).rateLimitPerMinute(rateLimitPerMinute)
                    .build();
        }

        TenantUsage usage = usageRepository.findByTenantIdAndUsageDate(tenantId, LocalDate.now())
                .orElse(null);
        int usedToday = (usage != null && usage.getApiCallsCount() != null) ? usage.getApiCallsCount() : 0;

        if (usedToday >= maxApiCallsPerDay) {
            return RateLimitResponse.builder()
                    .allowed(false).remaining(0).limit(maxApiCallsPerDay).rateLimitPerMinute(rateLimitPerMinute)
                    .build();
        }

        usageTrackingService.incrementApiCalls(tenantId);
        int remaining = Math.max(0, maxApiCallsPerDay - (usedToday + 1));
        return RateLimitResponse.builder()
                .allowed(true).remaining(remaining).limit(maxApiCallsPerDay).rateLimitPerMinute(rateLimitPerMinute)
                .build();
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
