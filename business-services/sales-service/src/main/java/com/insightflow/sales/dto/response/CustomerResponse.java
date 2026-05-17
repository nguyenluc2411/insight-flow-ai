package com.insightflow.sales.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CustomerResponse {
    private UUID id;
    private UUID tenantId;
    private String phone;
    private String email;
    private String fullName;
    private String gender;
    private LocalDate birthDate;
    private String rfmSegment;
    private BigDecimal totalSpent;
    private int orderCount;
    private Instant lastOrderAt;
    private Instant createdAt;
}
