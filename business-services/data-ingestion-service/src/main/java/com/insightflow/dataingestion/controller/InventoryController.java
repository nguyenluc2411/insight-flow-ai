package com.insightflow.dataingestion.controller;

import com.insightflow.dataingestion.dto.response.WorkspaceInventoryResponse;
import com.insightflow.dataingestion.service.IngestionService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final IngestionService ingestionService;

    @GetMapping("/workspace/{id}")
    public ResponseEntity<WorkspaceInventoryResponse> getInventoriesByWorkspace(
            @PathVariable("id") String workspaceId,
            @CurrentUser UserContext userContext,
            // Hứng thêm Header nội bộ từ thằng Python AI
            @RequestHeader(value = "X-Tenant-Id", required = false) String internalTenantId) {

        String tenantId;

        if (userContext != null && userContext.tenantId() != null) {
            // Trường hợp 1: Có JWT (Frontend gọi)
            tenantId = userContext.tenantId().toString();
        } else if (internalTenantId != null && !internalTenantId.isBlank()) {
            // Trường hợp 2: Không có JWT, nhưng có Header nội bộ (Python ML Service gọi)
            tenantId = internalTenantId;
            log.info("🤖 [INTERNAL CALL] Nhận Request từ AI Service bằng Header X-Tenant-Id: {}", tenantId);
        } else {
            // Chặn đứng ngay lập tức nếu không có gì cả
            log.warn("🚨 Từ chối truy cập: Không có JWT và không có Internal Header!");
            return ResponseEntity.status(401).build();
        }

        log.info("📤 Có Request xuất dữ liệu kho hàng cho Workspace: {} (tenant {})", workspaceId, tenantId);
        WorkspaceInventoryResponse response = ingestionService.exportWorkspaceData(tenantId, workspaceId);

        log.info("📦 Đã gom thành công: {} sản phẩm, {} biến thể, {} dòng tồn kho.",
                response.getProducts().size(), response.getVariants().size(), response.getInventoryFacts().size());

        return ResponseEntity.ok(response);
    }
}