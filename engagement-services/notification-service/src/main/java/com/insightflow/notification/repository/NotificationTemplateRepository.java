package com.insightflow.notification.repository;

import com.insightflow.notification.entity.NotificationTemplate;
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
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByTemplateKeyAndChannelAndActiveTrue(
            String templateKey,
            NotificationChannel channel);

    List<NotificationTemplate> findByNotificationTypeAndActiveTrue(NotificationType notificationType);

    boolean existsByTemplateKey(String templateKey);

    Page<NotificationTemplate> findByActiveTrue(Pageable pageable);
}
