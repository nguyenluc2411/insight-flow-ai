package com.insightflow.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterTenantRequest {

    @NotBlank
    private String tenantName;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9-]{3,50}$", message = "Slug must be 3-50 lowercase alphanumeric characters or hyphens")
    private String slug;

    @NotBlank
    @Email
    private String ownerEmail;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String ownerPassword;

    @NotBlank
    private String ownerFullName;
}
