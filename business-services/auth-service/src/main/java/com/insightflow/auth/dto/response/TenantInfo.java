package com.insightflow.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TenantInfo {

    private UUID id;
    private String name;
    private String slug;
    private String plan;
    private Instant trialEndsAt;
}
