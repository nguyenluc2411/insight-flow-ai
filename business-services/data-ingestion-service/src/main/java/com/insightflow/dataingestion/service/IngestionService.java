package com.insightflow.dataingestion.service;

import com.insightflow.dataingestion.dto.event.EventEnvelope;
import com.insightflow.dataingestion.dto.event.InventoryFileUploadedPayload;
import com.insightflow.dataingestion.dto.response.WorkspaceInventoryResponse;

public interface IngestionService {
    void handleFileUploadedEvent(EventEnvelope<InventoryFileUploadedPayload> envelope);
    WorkspaceInventoryResponse exportWorkspaceData(String workspaceId);
}