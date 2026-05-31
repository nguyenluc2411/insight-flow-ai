package com.insightflow.userworkspace.dto.event;

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
    @JsonProperty("workspace_id")
    private String workspaceId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("file_name")
    private String fileName; // Bổ sung đồng bộ với con 8082
    @JsonProperty("s3_file_url")
    private String s3FileUrl;
}