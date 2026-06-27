package com.insightflow.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "url", length = 1024)
    private String url;

    @Column(name = "utm_source")
    private String utmSource;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
