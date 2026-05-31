package com.insightflow.userworkspace.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.userworkspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final WorkspaceService workspaceService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.recommendation.generated", groupId = "workspace-group")
    public void handleRecommendationGenerated(String message) {
        log.info("🔥 [KAFKA] Đã nhận tín hiệu AI phân tích xong từ 8084!"); // Dòng này báo hiệu Kafka thông suốt
        try {
            JsonNode root = objectMapper.readTree(message);
            String workspaceId = root.path("payload").path("workspace_id").asText();

            if (workspaceId != null && !workspaceId.isEmpty()) {
                workspaceService.updateStatus(workspaceId, "COMPLETED", null);
                log.info("✅ Đã cập nhật DB thành COMPLETED cho Workspace: {}", workspaceId);
            } else {
                log.warn("⚠️ Payload Kafka không chứa workspace_id: {}", message);
            }
        } catch (Exception e) {
            log.error("❌ Lỗi parse Kafka Message: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "inventory.ingestion.failed", groupId = "workspace-group")
    public void handleIngestionFailed(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String workspaceId = root.path("payload").path("workspace_id").asText();
            String errorMsg = root.path("payload").path("error_message").asText();
            workspaceService.updateStatus(workspaceId, "FAILED", errorMsg);
        } catch (Exception e) {}
    }

    @KafkaListener(topics = "inventory.recommendation.failed", groupId = "workspace-group")
    public void handleRecommendationFailed(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String workspaceId = root.path("payload").path("workspace_id").asText();
            String errorMsg = root.path("payload").path("error_message").asText();
            workspaceService.updateStatus(workspaceId, "FAILED", errorMsg);
        } catch (Exception e) {}
    }
}