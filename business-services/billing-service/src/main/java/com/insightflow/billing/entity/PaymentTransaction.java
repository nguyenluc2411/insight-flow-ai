package com.insightflow.billing.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "billing_db", name = "payment_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ID từ SePay để đối soát và chống gọi trùng (Idempotency)
    @Column(name = "sepay_id", unique = true, nullable = false, length = 100)
    private String sepayId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "package_code", length = 50)
    private String packageCode;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "sender_account_number", length = 50)
    private String senderAccountNumber;

    @Column(columnDefinition = "TEXT")
    private String content;

    // Trạng thái: PENDING, SUCCESS, FAILED_VALIDATION, REFUNDED
    @Column(length = 30)
    private String status;

    @Column(name = "error_reason", columnDefinition = "TEXT")
    private String errorReason;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "transaction_code", unique = true, length = 50)
    private String transactionCode;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}