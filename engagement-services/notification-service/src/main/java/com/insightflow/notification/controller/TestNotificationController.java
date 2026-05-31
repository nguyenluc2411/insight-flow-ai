package com.insightflow.notification.controller;

import com.insightflow.notification.dto.response.ApiResponse;
import com.insightflow.common.events.notification.IncomingNotificationEvent;
import com.insightflow.notification.producer.NotificationEventProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ApiResponse<Map<String, Object>> sendRealtimeTest(@Valid @RequestBody IncomingNotificationEvent request) {
        if (request.recipientId() == null) {
            throw new IllegalArgumentException("recipientId is required");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }

        UUID eventId = request.eventId() != null ? request.eventId() : UUID.randomUUID();
        UUID correlationId = request.correlationId() != null ? request.correlationId() : UUID.randomUUID();
        Instant timestamp = request.timestamp() != null ? request.timestamp() : Instant.now();

        IncomingNotificationEvent event = IncomingNotificationEvent.builder()
                .eventId(eventId)
                .eventType(request.eventType())
                .timestamp(timestamp)
                .recipientId(request.recipientId())
                .recipientEmail(request.recipientEmail())
                .severity(request.severity())
                .title(request.title())
                .message(request.message())
                .productId(request.productId())
                .warehouseId(request.warehouseId())
                .correlationId(correlationId)
                .sourceService(request.sourceService())
                .build();

        log.info("Test realtime event publishing eventId={} correlationId={} recipientId={} recipientEmail={}",
                event.eventId(),
                event.correlationId(),
                event.recipientId(),
                event.recipientEmail());

        notificationEventProducer.publishBySeverity(event);

        return ApiResponse.success(
                Map.of(
                        "eventId", event.eventId(),
                        "correlationId", event.correlationId(),
                        "recipientId", event.recipientId(),
                        "recipientEmail", event.recipientEmail()
                ),
                "Test notification published");
    }
}

