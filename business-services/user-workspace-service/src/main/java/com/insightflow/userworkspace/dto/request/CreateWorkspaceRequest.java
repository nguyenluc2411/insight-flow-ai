package com.insightflow.userworkspace.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkspaceRequest {
    @NotBlank(message = "Tên phiên làm việc không được trống")
    @JsonProperty("workspace_name")
    private String workspaceName;

    @NotBlank(message = "Tên file không được trống")
    @JsonProperty("file_name")
    private String fileName;

    @NotBlank(message = "Định dạng file không được trống")
    @JsonProperty("content_type")
    private String contentType;
}