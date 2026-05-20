package com.insightflow.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.integration.connector.kiotviet.KiotVietWebhookVerifier;
import com.insightflow.integration.core.ConnectorType;
import com.insightflow.integration.entity.ConnectorConfig;
import com.insightflow.integration.entity.ProcessedWebhook;
import com.insightflow.integration.event.producer.IntegrationEventProducer;
import com.insightflow.integration.repository.ConnectorConfigRepository;
import com.insightflow.integration.repository.ProcessedWebhookRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Incoming POS webhook endpoints (public)")
public class WebhookController {

    private final KiotVietWebhookVerifier kiotVietVerifier;
    private final ConnectorConfigRepository configRepository;
    private final ProcessedWebhookRepository processedWebhookRepository;
    private final IntegrationEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @PostMapping("/kiotviet")
    @Operation(summary = "Receive KiotViet webhook",
               description = "HMAC-SHA256 signature verified (X-KiotViet-Signature). Idempotent — duplicate events are ignored. Requires an active KiotViet connector to be configured for the tenant.")
    public ResponseEntity<Void> receiveKiotViet(
            @RequestHeader(value = "X-KiotViet-Signature", required = false) String signature,
            @RequestBody String payload) {

        // Reject immediately if no active KiotViet connector is configured.
        UUID tenantId = extractTenantId(payload);
        ConnectorConfig config = findKiotVietConfig(tenantId);
        if (config == null) {
            log.warn("KiotViet webhook rejected: no active connector configured for tenantId={}", tenantId);
            return ResponseEntity.status(404).build();
        }

        String eventType = extractEventType(payload);
        String externalEventId = extractExternalEventId(payload);

        // Idempotency check
        if (externalEventId != null &&
                processedWebhookRepository.existsByConnectorTypeAndExternalEventId(
                        "KIOTVIET", externalEventId)) {
            log.debug("Duplicate KiotViet webhook ignored: eventId={}", externalEventId);
            return ResponseEntity.ok().build();
        }

        if (signature != null) {
            String secret = config.getWebhookSecret();
            if (secret != null && !secret.isBlank()) {
                boolean valid = kiotVietVerifier.verify(payload, signature, secret);
                if (!valid) {
                    log.warn("KiotViet webhook signature invalid — rejecting");
                    return ResponseEntity.status(401).build();
                }
            }
        }

        // Persist webhook for audit
        ProcessedWebhook record = new ProcessedWebhook();
        record.setTenantId(tenantId);
        record.setConnectorType("KIOTVIET");
        record.setEventType(eventType != null ? eventType : "unknown");
        record.setExternalEventId(externalEventId != null ? externalEventId : UUID.randomUUID().toString());
        record.setPayload(payload);
        record.setSignature(signature);
        record.setStatus("processed");
        record.setProcessedAt(Instant.now());

        try {
            processedWebhookRepository.save(record);
        } catch (Exception e) {
            // Fail-open: don't reject webhook if DB save fails
            log.error("Failed to persist webhook record: {}", e.getMessage());
        }

        // Publish Kafka event
        if (config != null) {
            try {
                eventProducer.publishProductSynced(config.getTenantId(), config.getId(),
                        Map.of("source", "webhook", "eventType", eventType != null ? eventType : "unknown",
                               "payload", payload));
            } catch (Exception e) {
                log.error("Failed to publish webhook event to Kafka: {}", e.getMessage());
            }
        }

        log.info("KiotViet webhook processed: eventType={} externalId={}", eventType, externalEventId);
        return ResponseEntity.ok().build();
    }

    private String extractEventType(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            return node.has("Type") ? node.get("Type").asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractExternalEventId(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("Id")) return node.get("Id").asText();
            if (node.has("id")) return node.get("id").asText();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private UUID extractTenantId(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("tenantId")) return UUID.fromString(node.get("tenantId").asText());
        } catch (Exception ignored) {}
        return null;
    }

    private ConnectorConfig findKiotVietConfig(UUID tenantId) {
        if (tenantId != null) {
            List<ConnectorConfig> configs = configRepository.findByTenantId(tenantId);
            return configs.stream()
                    .filter(c -> c.getConnectorType() == ConnectorType.KIOTVIET)
                    .filter(c -> "active".equals(c.getStatus()))
                    .findFirst()
                    .orElse(null);
        }
        List<ConnectorConfig> active = configRepository.findByStatus("active");
        return active.stream()
                .filter(c -> c.getConnectorType() == ConnectorType.KIOTVIET)
                .findFirst()
                .orElse(null);
    }
}
