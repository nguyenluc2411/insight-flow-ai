package com.insightflow.notification.service.impl;

import com.insightflow.notification.dto.request.NotificationInboxFilterRequest;
import com.insightflow.notification.dto.response.NotificationResponse;
import com.insightflow.notification.dto.response.UnreadCountResponse;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.entity.NotificationDeliveryHistory;
import com.insightflow.notification.enums.InboxStatus;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.NotificationSeverity;
import com.insightflow.notification.exception.ResourceNotFoundException;
import com.insightflow.notification.mapper.NotificationMapper;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.notification.service.interfaces.NotificationInboxService;
import com.insightflow.notification.service.interfaces.NotificationUnreadCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationInboxServiceImpl implements NotificationInboxService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationUnreadCounterService unreadCounterService;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getInbox(UUID recipientId, NotificationInboxFilterRequest filter, Pageable pageable) {
        Specification<Notification> spec = buildSpecification(recipientId, filter);
        return notificationRepository.findAll(spec, pageable).map(notificationMapper::toResponse);
    }

    @Override
    public UnreadCountResponse getUnreadCount(UUID recipientId) {
        long count = unreadCounterService.getUnreadCount(recipientId);
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID id, UUID recipientId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));

        if (notification.getInboxStatus() == InboxStatus.UNREAD) {
            notification.setInboxStatus(InboxStatus.READ);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
            unreadCounterService.decrementUnread(recipientId);
            log.info("Notification marked read notificationId={} recipientId={} correlationId={}",
                    id, recipientId, notification.getCorrelationId());
        }
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public UnreadCountResponse markAllAsRead(UUID recipientId) {
        int updated = notificationRepository.markAllAsRead(
                recipientId,
                InboxStatus.UNREAD,
                InboxStatus.READ,
                Instant.now());

        long count = unreadCounterService.resetUnread(recipientId);
        log.info("Notifications marked read-all recipientId={} updated={} unreadCount={}",
                recipientId, updated, count);
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public NotificationResponse archive(UUID id, UUID recipientId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));

        if (notification.getInboxStatus() != InboxStatus.ARCHIVED) {
            if (notification.getInboxStatus() == InboxStatus.UNREAD) {
                unreadCounterService.decrementUnread(recipientId);
            }
            notification.setInboxStatus(InboxStatus.ARCHIVED);
            notification.setArchivedAt(Instant.now());
            notificationRepository.save(notification);
            log.info("Notification archived notificationId={} recipientId={} correlationId={}",
                    id, recipientId, notification.getCorrelationId());
        }
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public NotificationResponse delete(UUID id, UUID recipientId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));

        if (notification.getInboxStatus() != InboxStatus.DELETED) {
            if (notification.getInboxStatus() == InboxStatus.UNREAD) {
                unreadCounterService.decrementUnread(recipientId);
            }
            notification.setInboxStatus(InboxStatus.DELETED);
            notification.setDeleted(true);
            notification.setDeletedAt(Instant.now());
            notificationRepository.save(notification);
            log.info("Notification deleted notificationId={} recipientId={} correlationId={}",
                    id, recipientId, notification.getCorrelationId());
        }
        return notificationMapper.toResponse(notification);
    }

    private Specification<Notification> buildSpecification(UUID recipientId, NotificationInboxFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("recipientId"), recipientId));
            predicates.add(cb.isFalse(root.get("deleted")));
            predicates.add(cb.notEqual(root.get("inboxStatus"), InboxStatus.DELETED));

            if (filter != null && Boolean.TRUE.equals(filter.getArchived())) {
                predicates.add(cb.equal(root.get("inboxStatus"), InboxStatus.ARCHIVED));
            } else if (filter != null && Boolean.TRUE.equals(filter.getUnread())) {
                predicates.add(cb.equal(root.get("inboxStatus"), InboxStatus.UNREAD));
            } else {
                predicates.add(root.get("inboxStatus").in(InboxStatus.UNREAD, InboxStatus.READ));
            }

            NotificationSeverity severity = filter != null ? filter.getSeverity() : null;
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }

            NotificationChannel channel = filter != null ? filter.getChannel() : null;
            if (channel != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<NotificationDeliveryHistory> deliveryRoot = subquery.from(NotificationDeliveryHistory.class);
                subquery.select(cb.literal(1L))
                        .where(
                                cb.equal(deliveryRoot.get("notification").get("id"), root.get("id")),
                                cb.equal(deliveryRoot.get("channel"), channel)
                        );
                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

