package com.insightflow.userworkspace.service;

import com.insightflow.userworkspace.dto.request.CreateWorkspaceRequest;
import com.insightflow.userworkspace.dto.response.CreateWorkspaceResponse;
import com.insightflow.userworkspace.dto.response.WorkspaceResponse;

import java.util.List;

public interface WorkspaceService {
    CreateWorkspaceResponse createWorkspace(CreateWorkspaceRequest request, String tenantId, String userId);

    void confirmUpload(String workspaceId, String tenantId);

    WorkspaceResponse getWorkspace(String workspaceId, String tenantId);

    void updateStatus(String workspaceId, String status, String errorMessage);

    List<WorkspaceResponse> getCompletedHistories(String tenantId);
}