package com.insightflow.userworkspace.service;


import com.insightflow.userworkspace.dto.request.CreateWorkspaceRequest;
import com.insightflow.userworkspace.dto.response.CreateWorkspaceResponse;
import com.insightflow.userworkspace.dto.response.WorkspaceResponse;

public interface WorkspaceService {
    CreateWorkspaceResponse createWorkspace(CreateWorkspaceRequest request);
    void confirmUpload(String workspaceId);
    WorkspaceResponse getWorkspace(String workspaceId);
    void updateRecommendation(String workspaceId, String recommendationText, Integer progress);
    void updateFailure(String workspaceId, String errorMessage);
}