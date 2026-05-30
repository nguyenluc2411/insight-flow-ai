package com.insightflow.notification.service.aggregation;

import com.insightflow.notification.entity.NotificationAggregationWindow;
import com.insightflow.notification.event.incoming.IncomingNotificationEvent;
import com.insightflow.notification.repository.NotificationAggregationWindowRepository;
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
        if (event == null) {
            return false;
        }

        String key = null;
        if (event.payload() != null && event.payload().get("aggregationKey") != null) {
            key = event.payload().get("aggregationKey").toString();
        }

        // Deterministic key so genuine duplicates (same recipient + event type)
        // actually group within a window. Previously used correlationId, which is
        // unique per event, so nothing ever grouped.
        if (key == null && event.recipientId() != null) {
            key = event.recipientId() + ":" + (event.eventType() != null ? event.eventType() : "GENERIC");
        }

        if (key == null) {
            return false;
        }

        Optional<NotificationAggregationWindow> existing = windowRepository.findFirstByAggregationKeyAndActiveTrue(key);

        if (existing.isPresent()) {
            NotificationAggregationWindow w = existing.get();
            if (w.getWindowEnd() != null && w.getWindowEnd().isAfter(Instant.now())) {
                w.setAggregatedCount(w.getAggregatedCount() + 1);
                windowRepository.save(w);
                log.info("Aggregated event into window key={} count={}", key, w.getAggregatedCount());
                // suppress until threshold reached
                return w.getAggregatedCount() < threshold;
            }
        }

        NotificationAggregationWindow window = new NotificationAggregationWindow();
        window.setAggregationKey(key);
        window.setNotificationType(event.payload() != null && event.payload().get("notificationType") != null
                ? com.insightflow.notification.enums.NotificationType.fromCode(event.payload().get("notificationType").toString())
                : com.insightflow.notification.enums.NotificationType.DASHBOARD_ALERT);
        window.setSeverity(event.severity());
        window.setWindowStart(Instant.now());
        window.setWindowEnd(Instant.now().plusSeconds(windowSeconds));
        window.setAggregatedCount(1);
        window.setActive(true);
        windowRepository.save(window);
        log.info("Created aggregation window key={} windowSeconds={} threshold={}", key, windowSeconds, threshold);

        // First event of a window is delivered (not suppressed); subsequent
        // same-key events within the window are folded/counted and suppressed.
        return false;
    }

    @Override
    public Optional<Long> getAggregatedCount(String aggregationKey) {
        return windowRepository.findFirstByAggregationKeyAndActiveTrue(aggregationKey)
                .map(w -> (long) w.getAggregatedCount());
    }
}
