package com.insightflow.sales.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SupplierResponse {
    private UUID id;
    private UUID tenantId;
    private String name;
    private String contactName;
    private String phone;
    private String email;
    private String address;
    private String status;
    private Instant createdAt;
}
