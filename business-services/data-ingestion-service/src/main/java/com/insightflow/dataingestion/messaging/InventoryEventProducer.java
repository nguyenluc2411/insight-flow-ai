package com.insightflow.dataingestion.messaging;

import com.insightflow.dataingestion.dto.event.EventEnvelope;
import com.insightflow.dataingestion.dto.event.InventoryIngestionFailedPayload;
import com.insightflow.dataingestion.dto.event.InventoryIngestionCompletedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendIngestionCompleted(EventEnvelope<InventoryIngestionCompletedPayload> envelope) {
        kafkaTemplate.send("inventory.ingestion.completed", envelope.getPayload().getWorkspaceId(), envelope);
    }

    public void sendFailed(EventEnvelope<InventoryIngestionFailedPayload> envelope) {
        kafkaTemplate.send("inventory.ingestion.failed", envelope.getPayload().getWorkspaceId(), envelope);
    }
}