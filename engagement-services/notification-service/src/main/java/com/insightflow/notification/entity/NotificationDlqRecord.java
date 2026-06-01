package com.insightflow.notification.entity;

import com.insightflow.notification.enums.FailureType;
import com.insightflow.notification.enums.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "notification_dlq_records",
        schema = "notification_db",
        indexes = {
                @Index(name = "idx_dlq_notification_id", columnList = "notification_id"),
                @Index(name = "idx_dlq_event_id", columnList = "event_id"),
                @Index(name = "idx_dlq_recipient_id", columnList = "recipient_id"),
                @Index(name = "idx_dlq_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
public class NotificationDlqRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id")
    private Notification notification;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "recipient_id")
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private NotificationChannel channel;

    @Column(name = "event_type", length = 120)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type", length = 20)
    private FailureType failureType;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "source_service", length = 100)
    private String sourceService;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

