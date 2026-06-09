package com.insightflow.userworkspace.controller;

import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import com.insightflow.userworkspace.dto.request.CreateWorkspaceRequest;
import com.insightflow.userworkspace.dto.response.CreateWorkspaceResponse;
import com.insightflow.userworkspace.dto.response.WorkspaceResponse;
import com.insightflow.userworkspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<CreateWorkspaceResponse> createWorkspace(
            @RequestBody CreateWorkspaceRequest request,
            @CurrentUser UserContext userContext) {

        if (userContext == null || userContext.tenantId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Không tìm thấy thông tin định danh đối tác (Tenant ID) hợp lệ.");
        }

        String tenantId = userContext.tenantId().toString();
        String userId = userContext.userId() != null ? userContext.userId().toString() : null;

        CreateWorkspaceResponse response = workspaceService.createWorkspace(request, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmUpload(
            @PathVariable("id") String workspaceId,
            @CurrentUser UserContext userContext) {

        if (userContext == null || userContext.tenantId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Không tìm thấy thông tin định danh đối tác (Tenant ID) hợp lệ.");
        }

        String tenantId = userContext.tenantId().toString();
        workspaceService.confirmUpload(workspaceId, tenantId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getWorkspace(
            @PathVariable("id") String workspaceId,
            @CurrentUser UserContext userContext) {

        if (userContext == null || userContext.tenantId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Không tìm thấy thông tin định danh đối tác (Tenant ID) hợp lệ.");
        }

        String tenantId = userContext.tenantId().toString();
        return ResponseEntity.ok(workspaceService.getWorkspace(workspaceId, tenantId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<WorkspaceResponse>> getCompletedHistories(
            @CurrentUser UserContext userContext) {

        if (userContext == null || userContext.tenantId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Không tìm thấy thông tin định danh đối tác (Tenant ID) hợp lệ.");
        }

        String tenantId = userContext.tenantId().toString();
        return ResponseEntity.ok(workspaceService.getCompletedHistories(tenantId));
    }
}