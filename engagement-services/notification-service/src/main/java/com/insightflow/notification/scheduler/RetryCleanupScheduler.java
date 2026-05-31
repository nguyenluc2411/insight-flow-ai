package com.insightflow.notification.scheduler;

import com.insightflow.notification.enums.RetryStatus;
import com.insightflow.notification.repository.NotificationRetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryCleanupScheduler {

    private final NotificationRetryRepository retryRepository;

    @Value("${notification.retry.cleanup.enabled:true}")
    private boolean enabled;

    @Value("${notification.retry.cleanup.retention-days:7}")
    private long retentionDays;

    @Scheduled(cron = "${notification.retry.cleanup.cron:0 0/30 * * * *}")
    public void cleanup() {
        if (!enabled) {
            return;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long deleted = retryRepository.deleteByRetryStatusInAndUpdatedAtBefore(
                List.of(RetryStatus.SUCCEEDED, RetryStatus.FAILED, RetryStatus.EXHAUSTED),
                cutoff);
        if (deleted > 0) {
            log.info("Retry cleanup archived topic=notification.retry.cleanup removed={} cutoff={} retentionDays={}",
                    deleted, cutoff, retentionDays);
        }
    }
}

