package com.insightflow.dataingestion.controller;

import com.insightflow.dataingestion.dto.response.WorkspaceInventoryResponse;
import com.insightflow.dataingestion.service.IngestionService;
import com.insightflow.security.UserContext;
import com.insightflow.security.UserContextHolder;
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
    public ResponseEntity<WorkspaceInventoryResponse> getInventoriesByWorkspace(@PathVariable("id") String workspaceId) {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.tenantId() == null) {
            return ResponseEntity.status(401).build();
        }
        String tenantId = ctx.tenantId().toString();
        log.info("📤 Có Request yêu cầu xuất dữ liệu kho hàng cho Workspace: {} (tenant {})", workspaceId, tenantId);

        WorkspaceInventoryResponse response = ingestionService.exportWorkspaceData(tenantId, workspaceId);

        log.info("📦 Đã gom thành công: {} sản phẩm, {} biến thể, {} dòng tồn kho. Đang gửi sang cho AI...",
                response.getProducts().size(), response.getVariants().size(), response.getInventoryFacts().size());

        return ResponseEntity.ok(response);
    }
}