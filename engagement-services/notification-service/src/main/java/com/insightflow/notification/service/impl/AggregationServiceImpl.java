package com.insightflow.notification.service.impl;

import com.insightflow.notification.entity.NotificationAggregationWindow;
import com.insightflow.notification.enums.NotificationSeverity;
import com.insightflow.common.events.notification.IncomingNotificationEvent;
import com.insightflow.notification.repository.NotificationAggregationWindowRepository;
import com.insightflow.notification.service.aggregation.AggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AggregationServiceImpl implements AggregationService {

    private final NotificationAggregationWindowRepository windowRepository;

    @Value("${notification.aggregation.window.seconds:60}")
    private long windowSeconds;

    @Value("${notification.aggregation.threshold:10}")
    private int threshold;

    @Override
    @Transactional
    public boolean tryAggregate(IncomingNotificationEvent event) {

        if (event == null) return false;

        NotificationSeverity severity = NotificationSeverity.fromCode(event.severity());
        if (severity == NotificationSeverity.HIGH) {
            log.info("HIGH severity bypass aggregation eventId={}", event.eventId());
            return false;
        }

        if (threshold <= 1) {
            log.info("Aggregation threshold <= 1, allowing immediate send key={} eventId={}",
                    event.recipientId() + "|" + event.eventType() + "|" + severity,
                    event.eventId());
            return false;
        }

        String key = event.recipientId() + "|" + event.eventType() + "|" + severity;

        Optional<NotificationAggregationWindow> existing =
                windowRepository.findFirstByAggregationKeyAndActiveTrue(key);

        if (existing.isPresent()) {

            NotificationAggregationWindow w = existing.get();

            if (w.getWindowEnd() != null && Instant.now().isBefore(w.getWindowEnd())) {

                int newCount = w.getAggregatedCount() + 1;
                w.setAggregatedCount(newCount);
                windowRepository.save(w);

                log.info("Aggregated event key={} count={}", key, newCount);

                if (newCount < threshold) {
                    return true; // suppress until threshold is reached
                }

                w.setActive(false);
                windowRepository.save(w);

                log.info("Threshold reached → allow sending key={}", key);
                return false; // allow send
            }

            windowRepository.delete(w);
        }

        NotificationAggregationWindow window = new NotificationAggregationWindow();
        window.setAggregationKey(key);
        window.setNotificationType(
                event.eventType() != null
                        ? com.insightflow.notification.enums.NotificationType.fromCode(event.eventType())
                        : com.insightflow.notification.enums.NotificationType.DASHBOARD_ALERT
        );

        window.setSeverity(severity);
        window.setWindowStart(Instant.now());
        window.setWindowEnd(Instant.now().plusSeconds(windowSeconds));
        window.setAggregatedCount(1);
        window.setActive(true);

        windowRepository.save(window);

        log.info("Created aggregation window key={}", key);

        return true; // first event is suppressed until threshold is reached
    }

    @Override
    public Optional<Long> getAggregatedCount(String aggregationKey) {
        return windowRepository.findFirstByAggregationKeyAndActiveTrue(aggregationKey)
                .map(w -> (long) w.getAggregatedCount());
    }
}

