package com.insightflow.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    // Required to identify which tenant this user belongs to.
    // B2B SaaS pattern: user must know their workspace slug.
    @NotBlank
    private String tenantSlug;
}
