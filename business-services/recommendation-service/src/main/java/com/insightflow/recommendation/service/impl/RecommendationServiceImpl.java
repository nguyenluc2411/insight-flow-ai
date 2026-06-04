package com.insightflow.recommendation.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.recommendation.ai.FashionAiClient;
import com.insightflow.recommendation.client.DataIngestionClient;
import com.insightflow.recommendation.dto.event.InventoryIngestionCompletedPayload;
import com.insightflow.recommendation.dto.response.WorkspaceInventoryResponse;
import com.insightflow.recommendation.entity.RecommendationHistory;
import com.insightflow.recommendation.messaging.RecommendationEventProducer;
import com.insightflow.recommendation.repository.RecommendationHistoryRepository;
import com.insightflow.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final DataIngestionClient dataIngestionClient;
    private final RecommendationHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;
    private final FashionAiClient aiClient;
    private final RecommendationEventProducer eventProducer;

    @Override
    public RecommendationHistory getRecommendationByWorkspace(String tenantId, String workspaceId) {
        return historyRepository.findTopByTenantIdAndWorkspaceIdOrderByCreatedAtDesc(tenantId, workspaceId)
                .orElseThrow(() -> new RuntimeException("Chưa có dữ liệu tư vấn nào cho Workspace này."));
    }

    @Override
    public void processRecommendation(InventoryIngestionCompletedPayload payload) {
        String tenantId = payload.getTenantId();
        String workspaceId = payload.getWorkspaceId();
        if (tenantId == null || tenantId.isBlank()) {
            log.error("❌ Bỏ qua event: thiếu tenant_id cho workspace {}", workspaceId);
            return;
        }
        boolean isProcessing = historyRepository.existsByWorkspaceIdAndStatus(workspaceId, "PROCESSING");
        if (isProcessing) {
            log.warn("⚠️ Bỏ qua event. Workspace {} đang được AI xử lý rồi!", workspaceId);
            return;
        }

        RecommendationHistory history = historyRepository.save(RecommendationHistory.builder()
                .tenantId(tenantId)
                .workspaceId(workspaceId)
                .status("PROCESSING")
                .createdAt(OffsetDateTime.now())
                .build());
        String rawAiResponse = null;

        try {
            WorkspaceInventoryResponse inventoryData = dataIngestionClient.exportWorkspaceData(workspaceId, tenantId);
            if (inventoryData == null || inventoryData.getProducts() == null || inventoryData.getProducts().isEmpty()) {
                throw new RuntimeException("Dữ liệu kho rỗng hoặc API 8082 không phản hồi.");
            }

            String compressedData = summarizeInventoryData(inventoryData);
            String missingFieldsStr = (payload.getMissingFields() != null) ? payload.getMissingFields().toString() : "[]";

            rawAiResponse = aiClient.generateInventoryStrategy(compressedData, payload.getCompletenessScore(), missingFieldsStr);

            // 🛡️ LỚP GIÁP BẢO VỆ: Tách riêng luồng parse JSON
            try {
                JsonNode parsedResult = objectMapper.readTree(rawAiResponse);
                history.setRecommendationResult(parsedResult.toString());
            } catch (Exception parseEx) {
                log.warn("⚠️ AI trả về kết quả có ký tự lạ (chưa chuẩn JSON 100%). Bỏ qua lỗi parse, tiến hành lưu thô!");
                history.setRecommendationResult(rawAiResponse);
            }

            // Đảm bảo chạy mượt đến đoạn này để báo DONE
            history.setStatus("DONE");
            history.setUpdatedAt(OffsetDateTime.now());
            historyRepository.save(history);

            // BÁO CHO 8081 BIẾT LUỒNG ĐÃ XONG MƯỢT MÀ
            eventProducer.sendRecommendationGenerated(workspaceId);
            log.info("✅ AI đã phân tích thành công cho Workspace {}", workspaceId);

        } catch (Exception e) {
            log.error("❌ Luồng AI thất bại cho Workspace {}: {}", workspaceId, e.getMessage());
            if (rawAiResponse != null) {
                log.error("🚨 DỮ LIỆU AI TRẢ VỀ: \n{}", rawAiResponse);
            }

            try {
                history.setStatus("ERROR");
                history.setErrorLog(e.getMessage());
                history.setUpdatedAt(OffsetDateTime.now());
                historyRepository.save(history);

                eventProducer.sendRecommendationFailed(workspaceId, e.getMessage());
            } catch (Exception dbException) {
                log.error("🔥 BÁO ĐỘNG ĐỎ: Database ngưng phản hồi!");
            }
        }
    }

    private String summarizeInventoryData(WorkspaceInventoryResponse data) {
        int totalProducts = data.getProducts() != null ? data.getProducts().size() : 0;
        int totalVariants = data.getVariants() != null ? data.getVariants().size() : 0;
        int totalStock = 0;
        if (data.getInventoryFacts() != null) {
            totalStock = data.getInventoryFacts().stream()
                    .mapToInt(f -> f.getQuantityInStock() != null ? f.getQuantityInStock() : 0)
                    .sum();
        }
        return String.format("Tổng sản phẩm: %d. Tổng SKU: %d. Tổng lượng tồn kho: %d chiếc.", totalProducts, totalVariants, totalStock);
    }
}