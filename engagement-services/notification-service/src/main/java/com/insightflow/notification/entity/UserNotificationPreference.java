package com.insightflow.notification.entity;

import com.insightflow.notification.enums.NotificationChannel;
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
        name = "user_notification_preferences",
        schema = "notification_db",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_preference_user_type_channel",
                        columnNames = {"user_id", "notification_type", "channel"})
        },
        indexes = {
                @Index(name = "idx_preference_user_id", columnList = "user_id"),
                @Index(name = "idx_preference_enabled", columnList = "enabled"),
                @Index(name = "idx_preference_type", columnList = "notification_type")
        }
)
@Getter
@Setter
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 60)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_severity", nullable = false, length = 20)
    private NotificationSeverity minSeverity = NotificationSeverity.LOW;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "mute_until")
    private Instant muteUntil;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
