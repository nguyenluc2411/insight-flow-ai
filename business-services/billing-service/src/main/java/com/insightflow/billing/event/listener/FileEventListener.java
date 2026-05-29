package com.insightflow.billing.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.billing.service.UsageTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileEventListener {

    private final UsageTrackingService usageTrackingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "file.uploaded", groupId = "billing-service-events")
    public void onFileUploaded(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode payload = objectMapper.readTree(record.value());
            UUID tenantId = UUID.fromString(payload.get("tenantId").asText());
            long sizeBytes = payload.has("sizeBytes") ? payload.get("sizeBytes").asLong() : 0L;
            usageTrackingService.updateStorageUsed(tenantId, sizeBytes);
            log.debug("Updated storage +{} bytes for tenantId={}", sizeBytes, tenantId);
        } catch (Exception e) {
            log.error("Failed to process file.uploaded event: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "file.deleted", groupId = "billing-service-events")
    public void onFileDeleted(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode payload = objectMapper.readTree(record.value());
            UUID tenantId = UUID.fromString(payload.get("tenantId").asText());
            long sizeBytes = payload.has("sizeBytes") ? payload.get("sizeBytes").asLong() : 0L;
            usageTrackingService.updateStorageUsed(tenantId, -sizeBytes);
            log.debug("Updated storage -{} bytes for tenantId={}", sizeBytes, tenantId);
        } catch (Exception e) {
            log.error("Failed to process file.deleted event: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
