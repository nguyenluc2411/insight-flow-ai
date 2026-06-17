package com.insightflow.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateTenantStatusRequest {

    @NotBlank
    @Pattern(regexp = "active|suspended|cancelled",
             message = "status must be one of: active, suspended, cancelled")
    private String status;
}
