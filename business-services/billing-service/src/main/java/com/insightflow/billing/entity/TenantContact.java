package com.insightflow.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thông tin liên hệ của chủ tenant (email + tên), cache lại từ sự kiện
 * {@code auth.tenant.registered} để billing gửi hóa đơn mà không phải gọi
 * đồng bộ sang auth-service.
 */
@Entity
@Table(schema = "billing_db", name = "tenant_contact")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantContact {

    /** Khóa chính = tenant_id (mỗi tenant 1 dòng liên hệ). */
    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
