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
public class WorkspaceResponse {
    private String id;
    @JsonProperty("user_id")
    private String userId;
    private String name;
    private String status;
    @JsonProperty("error_message")
    private String errorMessage;
    private Integer progress;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
}