package com.insightflow.notification.repository;

import com.insightflow.notification.entity.NotificationDeliveryHistory;
import com.insightflow.notification.enums.DeliveryStatus;
import com.insightflow.notification.enums.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationDeliveryHistoryRepository extends JpaRepository<NotificationDeliveryHistory, UUID> {

    Page<NotificationDeliveryHistory> findByNotification_Id(UUID notificationId, Pageable pageable);

    Optional<NotificationDeliveryHistory> findFirstByNotification_IdAndChannelOrderByCreatedAtDesc(
            UUID notificationId,
            NotificationChannel channel);

    long countByChannelAndDeliveryStatus(NotificationChannel channel, DeliveryStatus deliveryStatus);

    List<NotificationDeliveryHistory> findByDeliveryStatusAndCreatedAtBefore(
            DeliveryStatus deliveryStatus,
            Instant cutoff);
}
