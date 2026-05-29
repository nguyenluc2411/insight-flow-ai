package com.insightflow.notification.entity;

import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.RetryStatus;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notification_retry_queue",
        schema = "notification_db",
        indexes = {
                @Index(name = "idx_retry_notification_id", columnList = "notification_id"),
                @Index(name = "idx_retry_status", columnList = "retry_status"),
                @Index(name = "idx_retry_next_retry_at", columnList = "next_retry_at"),
                @Index(name = "idx_retry_channel", columnList = "channel")
        }
)
@Getter
@Setter
public class NotificationRetry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "retry_status", nullable = false, length = 20)
    private RetryStatus retryStatus = RetryStatus.SCHEDULED;

    @Column(name = "retry_attempt", nullable = false)
    private int retryAttempt;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "last_failure_reason", columnDefinition = "TEXT")
    private String lastFailureReason;

    @Column(name = "last_tried_at")
    private Instant lastTriedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
