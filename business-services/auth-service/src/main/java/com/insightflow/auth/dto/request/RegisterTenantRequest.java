package com.insightflow.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterTenantRequest {

    @NotBlank
    @Size(min = 2, max = 255)
    private String tenantName;

    @NotBlank
    @Pattern(
            regexp = "^[a-z0-9][a-z0-9\\-]{1,48}[a-z0-9]$",
            message = "Slug must be 3–50 lowercase alphanumeric characters and hyphens"
    )
    private String tenantSlug;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Password must contain at least one letter and one number"
    )
    private String password;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;
}
