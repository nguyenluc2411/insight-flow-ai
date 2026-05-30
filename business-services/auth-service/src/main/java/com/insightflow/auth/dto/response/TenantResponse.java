package com.insightflow.auth.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {

    private UUID id;
    private String name;
    private String slug;
    private String plan;
    private String status;
    private Instant createdAt;
}
