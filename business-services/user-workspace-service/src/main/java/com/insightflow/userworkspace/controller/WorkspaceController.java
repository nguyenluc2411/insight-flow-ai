package com.insightflow.userworkspace.controller;

import com.insightflow.userworkspace.dto.request.CreateWorkspaceRequest;
import com.insightflow.userworkspace.dto.response.ApiResponse;
import com.insightflow.userworkspace.dto.response.CreateWorkspaceResponse;
import com.insightflow.userworkspace.dto.response.WorkspaceResponse;
import com.insightflow.userworkspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateWorkspaceResponse>> createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        CreateWorkspaceResponse response = workspaceService.createWorkspace(request);
        return ResponseEntity.ok(ApiResponse.<CreateWorkspaceResponse>builder()
                .success(true)
                .message("Workspace created")
                .data(response)
                .errorCode(null)
                .timestamp(OffsetDateTime.now().toString())
                .build());
    }

    @PostMapping("/{id}/confirm-upload")
    public ResponseEntity<ApiResponse<Object>> confirmUpload(@PathVariable("id") String workspaceId) {
        workspaceService.confirmUpload(workspaceId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Upload confirmed")
                .data(null)
                .errorCode(null)
                .timestamp(OffsetDateTime.now().toString())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspace(@PathVariable("id") String workspaceId) {
        WorkspaceResponse response = workspaceService.getWorkspace(workspaceId);
        return ResponseEntity.ok(ApiResponse.<WorkspaceResponse>builder()
                .success(true)
                .message("OK")
                .data(response)
                .errorCode(null)
                .timestamp(OffsetDateTime.now().toString())
                .build());
    }

    @PatchMapping("/{id}/result")
    public ResponseEntity<ApiResponse<Object>> updateResult(@PathVariable("id") String workspaceId,
                                                            @RequestParam("recommendation_text") String recommendationText,
                                                            @RequestParam(value = "progress", required = false) Integer progress) {
        workspaceService.updateRecommendation(workspaceId, recommendationText, progress);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Updated")
                .data(null)
                .errorCode(null)
                .timestamp(OffsetDateTime.now().toString())
                .build());
    }

    @PatchMapping("/{id}/fail")
    public ResponseEntity<ApiResponse<Object>> updateFail(@PathVariable("id") String workspaceId,
                                                          @RequestParam("error_message") String errorMessage) {
        workspaceService.updateFailure(workspaceId, errorMessage);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Updated")
                .data(null)
                .errorCode(null)
                .timestamp(OffsetDateTime.now().toString())
                .build());
    }
}