package com.insightflow.auth.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TenantRegisteredEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventType = "auth.tenant.registered";

    private String tenantId;
    private String tenantSlug;
    private String plan;
    private String ownerId;

    @Builder.Default
    private Instant occurredAt = Instant.now();
}
