package com.insightflow.billing.service;

import com.insightflow.billing.dto.response.EntitlementResponse;
import com.insightflow.billing.dto.response.FeatureAccessResponse;
import com.insightflow.billing.entity.TenantSubscription;
import com.insightflow.billing.repository.TenantSubscriptionRepository;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitlementService {

    private static final String CACHE_KEY_PREFIX = "billing:entitlements:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final TenantSubscriptionRepository subscriptionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public List<String> getFeatureCodes(UUID tenantId) {
        String cacheKey = CACHE_KEY_PREFIX + tenantId;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List<?> cachedList) {
            log.debug("Cache hit for entitlements tenantId={}", tenantId);
            return (List<String>) cachedList;
        }

        TenantSubscription subscription = subscriptionRepository.findActiveOrTrialByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for tenant: " + tenantId));

        List<String> featureCodes = subscription.getFeaturesAtSubscription();
        if (featureCodes == null) featureCodes = List.of();

        redisTemplate.opsForValue().set(cacheKey, featureCodes, CACHE_TTL);
        return featureCodes;
    }

    public boolean hasFeature(UUID tenantId, String featureCode) {
        try {
            List<String> codes = getFeatureCodes(tenantId);
            return codes.contains(featureCode);
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    public EntitlementResponse getEntitlements(UUID tenantId) {
        String cacheKey = CACHE_KEY_PREFIX + tenantId;
        boolean isCached = redisTemplate.hasKey(cacheKey);

        TenantSubscription subscription = subscriptionRepository.findActiveOrTrialByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for tenant: " + tenantId));

        List<String> featureCodes = getFeatureCodes(tenantId);

        return EntitlementResponse.builder()
                .tenantId(tenantId)
                .subscriptionStatus(subscription.getStatus())
                .featureCodes(featureCodes)
                .cached(isCached)
                .build();
    }

    public FeatureAccessResponse checkFeatureAccess(UUID tenantId, String featureCode) {
        boolean hasAccess = hasFeature(tenantId, featureCode);
        return FeatureAccessResponse.builder()
                .featureCode(featureCode)
                .hasAccess(hasAccess)
                .reason(hasAccess ? "Feature included in subscription" : "Feature not included in current plan")
                .build();
    }

    public void evictCache(UUID tenantId) {
        String cacheKey = CACHE_KEY_PREFIX + tenantId;
        redisTemplate.delete(cacheKey);
        log.info("Evicted entitlement cache for tenantId={}", tenantId);
    }
}
