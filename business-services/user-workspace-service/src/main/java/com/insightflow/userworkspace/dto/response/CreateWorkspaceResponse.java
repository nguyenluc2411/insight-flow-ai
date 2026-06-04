package com.insightflow.userworkspace.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkspaceResponse {
    @JsonProperty("workspace_id")
    private String workspaceId;

    @JsonProperty("s3_presigned_url")
    private String s3PresignedUrl;
}