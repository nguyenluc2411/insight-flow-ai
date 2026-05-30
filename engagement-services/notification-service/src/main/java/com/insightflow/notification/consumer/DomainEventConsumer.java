package com.insightflow.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.common.events.ml.ForecastGeneratedEvent;
import com.insightflow.common.events.ml.RecommendationCreatedEvent;
import com.insightflow.notification.enums.NotificationSeverity;
import com.insightflow.notification.event.incoming.IncomingNotificationEvent;
import com.insightflow.notification.producer.NotificationEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bridges system domain events (ml.*) into the notification pipeline by mapping
 * each to an IncomingNotificationEvent and publishing via
 * NotificationEventProducer.publishBySeverity() — reusing the existing
 * dedup -> aggregate -> persist -> deliver flow.
 *
 * NOTE (multi-tenancy): domain events are tenant-scoped while the notification
 * model is recipient(user)-scoped. recipientId is set to tenantId as a
 * placeholder; resolving tenant -> owner user(s) is a follow-up.
 * catalog.inventory.updated is intentionally NOT bridged (fires on every stock
 * movement — would be noisy without a low-stock threshold rule).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventConsumer {

    private final NotificationEventProducer producer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "ml.recommendation.created",
            groupId = "notification-service-domain",
            containerFactory = "domainEventKafkaListenerContainerFactory")
    public void onRecommendationCreated(String raw, Acknowledgment ack) {
        try {
            RecommendationCreatedEvent e = objectMapper.readValue(raw, RecommendationCreatedEvent.class);
            Map<String, Object> payload = new HashMap<>();
            payload.put("variantId", e.getVariantId());
            payload.put("action", e.getAction());
            payload.put("reason", e.getReason());
            if (e.getSuggestedDiscount() != null) payload.put("suggestedDiscount", e.getSuggestedDiscount());

            // eventType must be a valid NotificationType code — the mapper resolves it
            // via NotificationType.fromCode(eventType).
            producer.publishBySeverity(IncomingNotificationEvent.builder()
                    .eventId(parseUuid(e.getEventId()))
                    .eventType(recommendationType(e.getAction()))
                    .timestamp(e.getOccurredAt() != null ? e.getOccurredAt() : Instant.now())
                    .recipientId(parseUuid(e.getTenantId()))
                    .severity(mapPriority(e.getPriority()))
                    .title(recommendationTitle(e.getAction()))
                    .message(e.getReason() != null ? e.getReason() : "Có đề xuất xử lý tồn kho mới.")
                    .productId(parseNullableUuid(e.getVariantId()))
                    .correlationId(UUID.randomUUID())
                    .sourceService("ml-service")
                    .payload(payload)
                    .build());
            log.debug("Bridged ml.recommendation.created eventId={}", e.getEventId());
        } catch (Exception ex) {
            log.error("Failed to bridge ml.recommendation.created: {}", ex.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "ml.forecast.generated",
            groupId = "notification-service-domain",
            containerFactory = "domainEventKafkaListenerContainerFactory")
    public void onForecastGenerated(String raw, Acknowledgment ack) {
        try {
            ForecastGeneratedEvent e = objectMapper.readValue(raw, ForecastGeneratedEvent.class);
            Map<String, Object> payload = new HashMap<>();
            payload.put("variantId", e.getVariantId());
            payload.put("totalForecastQty", e.getTotalForecastQty());
            payload.put("forecastDate", e.getForecastDate());

            producer.publishBySeverity(IncomingNotificationEvent.builder()
                    .eventId(parseUuid(e.getEventId()))
                    .eventType("DASHBOARD_ALERT")
                    .timestamp(e.getOccurredAt() != null ? e.getOccurredAt() : Instant.now())
                    .recipientId(parseUuid(e.getTenantId()))
                    .severity(NotificationSeverity.LOW)
                    .title("Dự báo nhu cầu mới")
                    .message("Đã có dự báo " + e.getTotalForecastQty() + " sản phẩm cho kỳ tới.")
                    .productId(parseNullableUuid(e.getVariantId()))
                    .correlationId(UUID.randomUUID())
                    .sourceService("ml-service")
                    .payload(payload)
                    .build());
            log.debug("Bridged ml.forecast.generated eventId={}", e.getEventId());
        } catch (Exception ex) {
            log.error("Failed to bridge ml.forecast.generated: {}", ex.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    private NotificationSeverity mapPriority(String priority) {
        if (priority == null) return NotificationSeverity.MEDIUM;
        return switch (priority.toUpperCase()) {
            case "HIGH" -> NotificationSeverity.HIGH;
            case "LOW" -> NotificationSeverity.LOW;
            default -> NotificationSeverity.MEDIUM;
        };
    }

    private String recommendationType(String action) {
        if (action == null) return "DASHBOARD_ALERT";
        return switch (action.toUpperCase()) {
            case "CLEARANCE" -> "CLEARANCE_RECOMMENDATION";
            case "RESTOCK" -> "RESTOCK_RECOMMENDATION";
            default -> "DASHBOARD_ALERT";
        };
    }

    private String recommendationTitle(String action) {
        if (action == null) return "Đề xuất mới";
        return switch (action.toUpperCase()) {
            case "CLEARANCE" -> "Đề xuất thanh lý tồn kho";
            case "RESTOCK" -> "Đề xuất nhập thêm hàng";
            case "PROMOTE" -> "Đề xuất đẩy bán";
            default -> "Đề xuất xử lý tồn kho";
        };
    }

    private UUID parseUuid(String s) {
        UUID u = parseNullableUuid(s);
        return u != null ? u : UUID.randomUUID();
    }

    private UUID parseNullableUuid(String s) {
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
