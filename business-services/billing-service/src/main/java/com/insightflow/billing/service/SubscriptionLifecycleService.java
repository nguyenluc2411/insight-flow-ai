package com.insightflow.billing.service;

import com.insightflow.billing.entity.TenantSubscription;
import com.insightflow.billing.repository.TenantSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Periodically expires Trials whose 30-day window has passed and downgrades the
 * tenant to FREE (data retained, account not locked).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionLifecycleService {

    private final TenantSubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    /** Runs daily at 01:00 (overridable via app.trial-expiry.cron). */
    @Scheduled(cron = "${app.trial-expiry.cron:0 0 1 * * *}")
    public void scheduledExpireTrials() {
        int n = expireTrials();
        if (n > 0) {
            log.info("Trial-expiry job downgraded {} expired trial(s) to FREE", n);
        }
    }

    /**
     * Finds TRIAL subscriptions whose end_date is before today and downgrades
     * each to FREE. Each tenant is processed in its own transaction so one
     * failure does not block the rest. Returns the number downgraded.
     * Intentionally NOT @Transactional — each downgradeToFree opens its own
     * transaction so one tenant's failure cannot roll back the others.
     */
    public int expireTrials() {
        List<TenantSubscription> expired =
                subscriptionRepository.findByStatusAndEndDateBefore("TRIAL", LocalDate.now());
        int count = 0;
        for (TenantSubscription trial : expired) {
            try {
                subscriptionService.downgradeToFree(trial.getTenantId());
                count++;
            } catch (Exception e) {
                log.error("Failed to expire trial for tenant [{}]: {}",
                        trial.getTenantId(), e.getMessage());
            }
        }
        return count;
    }
}
