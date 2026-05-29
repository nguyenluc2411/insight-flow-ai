package com.insightflow.billing.service;

import com.insightflow.billing.dto.request.DowngradeRequest;
import com.insightflow.billing.dto.request.UpgradeRequest;
import com.insightflow.billing.dto.response.InternalSubscriptionResponse;
import com.insightflow.billing.dto.response.SubscriptionResponse;
import com.insightflow.billing.entity.BillingPackage;
import com.insightflow.billing.entity.OutboxEvent;
import com.insightflow.billing.entity.Plan;
import com.insightflow.billing.entity.PlanLimit;
import com.insightflow.billing.entity.TenantSubscription;
import com.insightflow.billing.repository.OutboxRepository;
import com.insightflow.billing.repository.PackageRepository;
import com.insightflow.billing.repository.PlanLimitRepository;
import com.insightflow.billing.repository.PlanRepository;
import com.insightflow.billing.repository.TenantSubscriptionRepository;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ErrorCode;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final TenantSubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PackageRepository packageRepository;
    private final PlanLimitRepository planLimitRepository;
    private final OutboxRepository outboxRepository;
    private final PackageService packageService;
    private final BillingHistoryService billingHistoryService;
    private final EntitlementService entitlementService;

    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(UUID tenantId) {
        TenantSubscription sub = subscriptionRepository.findActiveOrTrialByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for tenant: " + tenantId));
        return toSubscriptionResponse(sub);
    }

    @Transactional(readOnly = true)
    public InternalSubscriptionResponse getInternalSubscription(UUID tenantId) {
        TenantSubscription sub = subscriptionRepository.findActiveOrTrialByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for tenant: " + tenantId));
        return InternalSubscriptionResponse.builder()
                .subscriptionId(sub.getId())
                .tenantId(sub.getTenantId())
                .planId(sub.getPlanId())
                .status(sub.getStatus())
                .featureCodes(sub.getFeaturesAtSubscription())
                .limits(sub.getLimitsAtSubscription())
                .planVersion(sub.getPlanVersion())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .autoRenew(sub.getAutoRenew())
                .build();
    }

    @Transactional
    public SubscriptionResponse upgradePlan(UUID tenantId, UpgradeRequest req) {
        Plan newPlan = planRepository.findById(req.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + req.getPlanId()));
        BillingPackage newPackage = packageRepository.findById(newPlan.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        // Get current subscription if any (may not exist for new tenants)
        String fromPackageCode = null;
        TenantSubscription currentSub = subscriptionRepository.findActiveOrTrialByTenantId(tenantId).orElse(null);
        if (currentSub != null) {
            // Expire old subscription
            currentSub.setStatus("EXPIRED");
            subscriptionRepository.save(currentSub);
            // Resolve from package code
            Plan oldPlan = planRepository.findById(currentSub.getPlanId()).orElse(null);
            if (oldPlan != null) {
                packageRepository.findById(oldPlan.getPackageId())
                        .ifPresent(p -> {});
                fromPackageCode = oldPlan != null
                        ? packageRepository.findById(oldPlan.getPackageId()).map(BillingPackage::getCode).orElse(null)
                        : null;
            }
        }

        // Snapshot features and limits at subscription time
        List<String> featureCodes = packageService.getFeatureCodesForPackage(newPackage.getId());
        Map<String, Object> limitsSnapshot = buildLimitsSnapshot(newPackage.getId());

        // Determine start/end dates
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = calculateEndDate(startDate, newPlan.getBillingCycle(), newPlan.getTrialDays());

        String status = "TRIAL".equalsIgnoreCase(newPlan.getBillingCycle()) ? "TRIAL" : "ACTIVE";

        TenantSubscription newSub = TenantSubscription.builder()
                .tenantId(tenantId)
                .planId(newPlan.getId())
                .priceAtSubscription(newPlan.getPriceVnd())
                .featuresAtSubscription(featureCodes)
                .limitsAtSubscription(limitsSnapshot)
                .planVersion(newPackage.getVersion())
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .autoRenew(req.getAutoRenew())
                .build();

        TenantSubscription saved = subscriptionRepository.save(newSub);

        // Record billing history
        billingHistoryService.recordEvent(tenantId, saved.getId(), "PLAN_CHANGED",
                fromPackageCode, newPackage.getCode(), newPlan.getPriceVnd(),
                "Upgraded to plan: " + newPackage.getName());

        // Publish via outbox
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("tenantId", tenantId.toString());
        eventPayload.put("subscriptionId", saved.getId().toString());
        eventPayload.put("newPackageCode", newPackage.getCode());
        eventPayload.put("status", status);

        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(tenantId)
                .eventType("subscription.changed")
                .payload(eventPayload)
                .build());

        // Evict entitlement cache
        entitlementService.evictCache(tenantId);

        log.info("Tenant [{}] upgraded to plan [{}] ({})", tenantId, newPackage.getCode(), newPlan.getBillingCycle());
        return toSubscriptionResponse(saved);
    }

    @Transactional
    public SubscriptionResponse downgradePlan(UUID tenantId, DowngradeRequest req) {
        Plan newPlan = planRepository.findById(req.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + req.getPlanId()));
        BillingPackage newPackage = packageRepository.findById(newPlan.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        TenantSubscription currentSub = subscriptionRepository.findActiveOrTrialByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for tenant: " + tenantId));

        // Expire current
        String fromPackageCode = null;
        Plan oldPlan = planRepository.findById(currentSub.getPlanId()).orElse(null);
        if (oldPlan != null) {
            fromPackageCode = packageRepository.findById(oldPlan.getPackageId())
                    .map(BillingPackage::getCode).orElse(null);
        }
        currentSub.setStatus("EXPIRED");
        subscriptionRepository.save(currentSub);

        List<String> featureCodes = packageService.getFeatureCodesForPackage(newPackage.getId());
        Map<String, Object> limitsSnapshot = buildLimitsSnapshot(newPackage.getId());

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = calculateEndDate(startDate, newPlan.getBillingCycle(), newPlan.getTrialDays());

        TenantSubscription newSub = TenantSubscription.builder()
                .tenantId(tenantId)
                .planId(newPlan.getId())
                .priceAtSubscription(newPlan.getPriceVnd())
                .featuresAtSubscription(featureCodes)
                .limitsAtSubscription(limitsSnapshot)
                .planVersion(newPackage.getVersion())
                .status("ACTIVE")
                .startDate(startDate)
                .endDate(endDate)
                .autoRenew(true)
                .build();

        TenantSubscription saved = subscriptionRepository.save(newSub);

        billingHistoryService.recordEvent(tenantId, saved.getId(), "PLAN_CHANGED",
                fromPackageCode, newPackage.getCode(), newPlan.getPriceVnd(),
                "Downgraded to plan: " + newPackage.getName() + (req.getReason() != null ? " - " + req.getReason() : ""));

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("tenantId", tenantId.toString());
        eventPayload.put("subscriptionId", saved.getId().toString());
        eventPayload.put("newPackageCode", newPackage.getCode());
        eventPayload.put("status", "ACTIVE");

        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(tenantId)
                .eventType("subscription.changed")
                .payload(eventPayload)
                .build());

        entitlementService.evictCache(tenantId);

        log.info("Tenant [{}] downgraded to plan [{}]", tenantId, newPackage.getCode());
        return toSubscriptionResponse(saved);
    }

    private Map<String, Object> buildLimitsSnapshot(UUID packageId) {
        return planLimitRepository.findByPackageId(packageId)
                .map(limit -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("max_api_calls_per_day", limit.getMaxApiCallsPerDay());
                    map.put("max_storage_gb", limit.getMaxStorageGb());
                    map.put("max_users", limit.getMaxUsers());
                    map.put("api_rate_limit_per_minute", limit.getApiRateLimitPerMinute());
                    return map;
                })
                .orElse(new HashMap<>());
    }

    private LocalDate calculateEndDate(LocalDate startDate, String billingCycle, Integer trialDays) {
        return switch (billingCycle.toUpperCase()) {
            case "MONTHLY" -> startDate.plusMonths(1);
            case "YEARLY" -> startDate.plusYears(1);
            case "TRIAL" -> startDate.plusDays(trialDays != null ? trialDays : 14);
            default -> startDate.plusMonths(1);
        };
    }

    private SubscriptionResponse toSubscriptionResponse(TenantSubscription sub) {
        return SubscriptionResponse.builder()
                .id(sub.getId())
                .tenantId(sub.getTenantId())
                .planId(sub.getPlanId())
                .priceAtSubscription(sub.getPriceAtSubscription())
                .featuresAtSubscription(sub.getFeaturesAtSubscription())
                .limitsAtSubscription(sub.getLimitsAtSubscription())
                .planVersion(sub.getPlanVersion())
                .status(sub.getStatus())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .autoRenew(sub.getAutoRenew())
                .createdAt(sub.getCreatedAt())
                .updatedAt(sub.getUpdatedAt())
                .build();
    }
}
