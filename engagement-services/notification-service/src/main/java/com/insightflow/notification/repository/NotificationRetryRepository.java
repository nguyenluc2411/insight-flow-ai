package com.insightflow.notification.repository;

import com.insightflow.notification.entity.NotificationRetry;
import com.insightflow.notification.enums.RetryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRetryRepository extends JpaRepository<NotificationRetry, UUID> {

    Page<NotificationRetry> findByRetryStatusAndNextRetryAtBefore(
            RetryStatus status,
            Instant cutoff,
            Pageable pageable);

    List<NotificationRetry> findByNotification_IdAndRetryStatusIn(
            UUID notificationId,
            Collection<RetryStatus> statuses);

    Optional<NotificationRetry> findFirstByNotification_IdAndRetryStatusOrderByNextRetryAtAsc(
            UUID notificationId,
            RetryStatus status);

    long countByRetryStatus(RetryStatus status);

    boolean existsByNotification_IdAndRetryStatusIn(
            UUID notificationId,
            Collection<RetryStatus> statuses);
}
