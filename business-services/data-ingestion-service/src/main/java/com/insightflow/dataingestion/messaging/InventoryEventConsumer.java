package com.insightflow.dataingestion.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.dataingestion.dto.event.EventEnvelope;
import com.insightflow.dataingestion.dto.event.InventoryFileUploadedPayload;
import com.insightflow.dataingestion.service.IngestionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class InventoryEventConsumer {

    private final IngestionService ingestionService;
    private final Executor taskExecutor;
    private final ObjectMapper objectMapper; // Thêm công cụ parse JSON

    public InventoryEventConsumer(IngestionService ingestionService,
                                  @Qualifier("taskExecutor") Executor taskExecutor,
                                  ObjectMapper objectMapper) {
        this.ingestionService = ingestionService;
        this.taskExecutor = taskExecutor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_FILE_UPLOADED)
    public void consumeFileUploaded(String rawJsonMessage) { // Nhận chuỗi JSON gốc
        try {
            // Dùng TypeReference để "chỉ tận tay, day tận trán" cho Jackson biết cái ruột là gì
            EventEnvelope<InventoryFileUploadedPayload> envelope = objectMapper.readValue(
                    rawJsonMessage,
                    new TypeReference<EventEnvelope<InventoryFileUploadedPayload>>() {}
            );

            System.out.println("🎉 ĐÃ NHẬN VÀ PARSE THÀNH CÔNG TIN NHẮN TỪ KAFKA: " + envelope.getPayload().getWorkspaceId());

            taskExecutor.execute(() -> ingestionService.handleFileUploadedEvent(envelope));

        } catch (Exception e) {
            System.err.println("❌ Lỗi Parse JSON: " + e.getMessage());
        }
    }
}