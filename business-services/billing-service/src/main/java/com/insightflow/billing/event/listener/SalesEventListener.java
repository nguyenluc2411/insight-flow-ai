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
public class SalesEventListener {

    private final UsageTrackingService usageTrackingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "sales.order.completed", groupId = "billing-service-events")
    public void onOrderCompleted(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode payload = objectMapper.readTree(record.value());
            UUID tenantId = UUID.fromString(payload.get("tenantId").asText());
            usageTrackingService.incrementReportsGenerated(tenantId);
            log.debug("Incremented reports_generated for tenantId={}", tenantId);
        } catch (Exception e) {
            log.error("Failed to process sales.order.completed event: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
