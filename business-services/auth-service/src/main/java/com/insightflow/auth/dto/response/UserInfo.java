package com.insightflow.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserInfo {

    private UUID id;
    private String email;
    private String fullName;
    private UUID tenantId;
    private String tenantSlug;
    private List<String> roles;
}
