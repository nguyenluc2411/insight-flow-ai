package com.insightflow.integration.event.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishProductSynced(UUID tenantId, UUID connectorId, Object productData) {
        publish("integration.product.synced", tenantId.toString(),
                buildEvent("integration.product.synced", tenantId, connectorId, productData));
    }

    public void publishOrderSynced(UUID tenantId, UUID connectorId, Object orderData) {
        publish("integration.order.synced", tenantId.toString(),
                buildEvent("integration.order.synced", tenantId, connectorId, orderData));
    }

    public void publishInventorySynced(UUID tenantId, UUID connectorId, Object inventoryData) {
        publish("integration.inventory.synced", tenantId.toString(),
                buildEvent("integration.inventory.synced", tenantId, connectorId, inventoryData));
    }

    public void publishSyncCompleted(UUID tenantId, UUID connectorId,
                                      String connectorType, String syncType,
                                      int recordsSynced) {
        Map<String, Object> payload = Map.of(
                "connectorType", connectorType,
                "syncType", syncType,
                "connectorId", connectorId.toString(),
                "recordsSynced", recordsSynced,
                "syncedTo", Instant.now().toString()
        );
        publish("integration.sync.completed", tenantId.toString(),
                buildEvent("integration.sync.completed", tenantId, connectorId, payload));
    }

    private void publish(String topic, String key, Map<String, Object> event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish Kafka event to topic={}: {}", topic, ex.getMessage());
                    } else {
                        log.debug("Kafka event published topic={} partition={} offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    private Map<String, Object> buildEvent(String eventType, UUID tenantId,
                                            UUID integrationId, Object payload) {
        return Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", eventType,
                "tenantId", tenantId.toString(),
                "integrationId", integrationId.toString(),
                "occurredAt", Instant.now().toString(),
                "payload", payload
        );
    }
}
