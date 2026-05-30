package com.insightflow.notification.repository;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.enums.InboxStatus;
import com.insightflow.notification.enums.NotificationStatus;
import com.insightflow.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {

    Page<Notification> findByRecipientId(UUID recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndInboxStatus(UUID recipientId, InboxStatus inboxStatus, Pageable pageable);

    Page<Notification> findByRecipientIdAndInboxStatusIn(
            UUID recipientId,
            Collection<InboxStatus> inboxStatuses,
            Pageable pageable);

    Page<Notification> findByRecipientIdAndStatus(UUID recipientId, NotificationStatus status, Pageable pageable);

    Optional<Notification> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    long countByRecipientIdAndInboxStatus(UUID recipientId, InboxStatus inboxStatus);

    long countByRecipientIdAndInboxStatusIn(UUID recipientId, Collection<InboxStatus> inboxStatuses);

    List<Notification> findTop50ByRecipientIdAndInboxStatusOrderByCreatedAtDesc(
            UUID recipientId,
            InboxStatus inboxStatus);

    @Query("select n.notificationType, count(n) from Notification n " +
            "where n.createdAt between :start and :end group by n.notificationType")
    List<Object[]> countByTypeBetween(Instant start, Instant end);

    @Query("select count(n) from Notification n " +
            "where n.notificationType = :type and n.createdAt between :start and :end")
    long countByTypeAndCreatedAtBetween(NotificationType type, Instant start, Instant end);

    // ---- Inbox read API (recipient-scoped, excludes soft-deleted) ----

    long countByRecipientIdAndInboxStatusAndDeletedFalse(UUID recipientId, InboxStatus inboxStatus);

    Optional<Notification> findByIdAndRecipientIdAndDeletedFalse(UUID id, UUID recipientId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.inboxStatus = com.insightflow.notification.enums.InboxStatus.READ, " +
            "n.readAt = :now " +
            "where n.recipientId = :recipientId " +
            "and n.inboxStatus = com.insightflow.notification.enums.InboxStatus.UNREAD " +
            "and n.deleted = false")
    int markAllReadForRecipient(@Param("recipientId") UUID recipientId, @Param("now") Instant now);
}
