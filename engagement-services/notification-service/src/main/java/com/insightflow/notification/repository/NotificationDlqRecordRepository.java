package com.insightflow.notification.repository;

import com.insightflow.notification.entity.NotificationDlqRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationDlqRecordRepository extends JpaRepository<NotificationDlqRecord, UUID> {

    Optional<NotificationDlqRecord> findFirstByNotification_IdOrderByCreatedAtDesc(UUID notificationId);
}

