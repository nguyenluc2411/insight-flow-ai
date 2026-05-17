package com.insightflow.userworkspace.messaging;


import com.insightflow.userworkspace.dto.event.EventEnvelope;
import com.insightflow.userworkspace.dto.event.InventoryFileUploadedPayload;
import com.insightflow.userworkspace.dto.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendFileUploaded(EventEnvelope<InventoryFileUploadedPayload> envelope) {
        kafkaTemplate.send(KafkaTopics.INVENTORY_FILE_UPLOADED, envelope.getPayload().getWorkspaceId(), envelope);
    }
}