package com.insightflow.notification.controller;

import com.insightflow.notification.dto.kafka.IncomingNotificationEventDto;
import com.insightflow.notification.dto.response.ApiResponse;
import com.insightflow.notification.event.incoming.IncomingNotificationEvent;
import com.insightflow.notification.producer.NotificationEventProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/test/notifications")
@RequiredArgsConstructor
@Validated
@Slf4j
public class TestNotificationController {

    private final NotificationEventProducer notificationEventProducer;

    @PostMapping("/realtime")
    public ApiResponse<Map<String, Object>> sendRealtimeTest(@Valid @RequestBody IncomingNotificationEventDto request) {
        if (request.getRecipientId() == null) {
            throw new IllegalArgumentException("recipientId is required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }

        if (request.getEventId() == null) {
            request.setEventId(UUID.randomUUID());
        }
        if (request.getCorrelationId() == null) {
            request.setCorrelationId(UUID.randomUUID());
        }
        if (request.getTimestamp() == null) {
            request.setTimestamp(Instant.now());
        }

        IncomingNotificationEvent event = IncomingNotificationEvent.builder()
                .eventId(request.getEventId())
                .eventType(request.getEventType())
                .timestamp(request.getTimestamp())
                .recipientId(request.getRecipientId())
                .severity(request.getSeverity())
                .title(request.getTitle())
                .message(request.getMessage())
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .correlationId(request.getCorrelationId())
                .sourceService(request.getSourceService())
                .payload(request.getPayload())
                .build();

        log.info("Test realtime event publishing eventId={} correlationId={} recipientId={}",
                event.eventId(),
                event.correlationId(),
                event.recipientId());

        notificationEventProducer.publishBySeverity(event);

        return ApiResponse.success(
                Map.of(
                        "eventId", event.eventId(),
                        "correlationId", event.correlationId(),
                        "recipientId", event.recipientId()
                ),
                "Test notification published");
    }
}
