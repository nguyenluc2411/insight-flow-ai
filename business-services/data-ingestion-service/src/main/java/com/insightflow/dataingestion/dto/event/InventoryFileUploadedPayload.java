package com.insightflow.dataingestion.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFileUploadedPayload {
    @JsonProperty("tenant_id")
    private String tenantId;
    @JsonProperty("workspace_id")
    private String workspaceId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("s3_file_url")
    private String s3FileUrl;
    @JsonProperty("file_name")
    private String fileName;
}