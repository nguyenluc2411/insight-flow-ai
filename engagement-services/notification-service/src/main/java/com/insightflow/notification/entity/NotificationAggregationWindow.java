package com.insightflow.notification.entity;

import com.insightflow.notification.enums.NotificationSeverity;
import com.insightflow.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notification_aggregation_windows",
        schema = "notification_db",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_aggregation_key_window",
                        columnNames = {"aggregation_key", "window_start", "window_end"})
        },
        indexes = {
                @Index(name = "idx_aggregation_key", columnList = "aggregation_key"),
                @Index(name = "idx_aggregation_window_start", columnList = "window_start"),
                @Index(name = "idx_aggregation_window_end", columnList = "window_end"),
                @Index(name = "idx_aggregation_active", columnList = "active")
        }
)
@Getter
@Setter
public class NotificationAggregationWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregation_key", nullable = false, length = 200)
    private String aggregationKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 60)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationSeverity severity;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;

    @Column(name = "aggregated_count", nullable = false)
    private int aggregatedCount = 0;

    @Column(name = "last_notification_id")
    private UUID lastNotificationId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
