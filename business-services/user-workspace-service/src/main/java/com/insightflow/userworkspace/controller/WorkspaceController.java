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
import java.util.List;
@CrossOrigin("*")
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
                .message("Khởi tạo phiên làm việc thành công, đường dẫn tải file S3 đã sẵn sàng")
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
                .message("Xác nhận file tải lên thành công, đang kích hoạt luồng AI phân tích dữ liệu")
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
                .message("Lấy thông tin phiên làm việc thành công")
                .data(response)
                .errorCode(null)
                .timestamp(OffsetDateTime.now().toString())
                .build());
    }

    @GetMapping("/user/history")
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> getHistory() {
        List<WorkspaceResponse> responses = workspaceService.getCompletedHistories();
        return ResponseEntity.ok(ApiResponse.<List<WorkspaceResponse>>builder()
                .success(true)
                .message("Lấy lịch sử phân tích thành công")
                .data(responses)
                .errorCode(null)
                .timestamp(OffsetDateTime.now().toString())
                .build());
    }
}