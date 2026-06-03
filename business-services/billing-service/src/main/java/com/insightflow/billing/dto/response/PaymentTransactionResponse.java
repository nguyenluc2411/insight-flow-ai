package com.insightflow.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentTransactionResponse {
    private UUID id;
    private String sepayId;
    private UUID tenantId;
    private String packageCode;
    private Integer amount;
    private String accountNumber;
    private String senderAccountNumber;
    private String content;
    private String status;
    private String errorReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}