package com.insightflow.notification.repository;

import com.insightflow.notification.entity.UserNotificationPreference;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, UUID> {

    Optional<UserNotificationPreference> findByUserIdAndNotificationTypeAndChannel(
            UUID userId,
            NotificationType notificationType,
            NotificationChannel channel);

    List<UserNotificationPreference> findByUserIdAndEnabledTrue(UUID userId);

    Page<UserNotificationPreference> findByUserId(UUID userId, Pageable pageable);

    boolean existsByUserIdAndNotificationTypeAndChannel(
            UUID userId,
            NotificationType notificationType,
            NotificationChannel channel);
}

