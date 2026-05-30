package com.insightflow.notification.service.impl;

import com.insightflow.notification.dto.response.NotificationResponse;
import com.insightflow.notification.dto.response.UnreadCountResponse;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.enums.InboxStatus;
import com.insightflow.notification.enums.NotificationType;
import com.insightflow.notification.mapper.NotificationMapper;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import com.insightflow.notification.service.interfaces.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> listNotifications(UUID recipientId, Boolean unreadOnly, String type, Pageable pageable) {
        Specification<Notification> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("recipientId"), recipientId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (Boolean.TRUE.equals(unreadOnly)) {
                predicates.add(cb.equal(root.get("inboxStatus"), InboxStatus.UNREAD));
            }
            NotificationType parsedType = parseType(type);
            if (parsedType != null) {
                predicates.add(cb.equal(root.get("notificationType"), parsedType));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return notificationRepository.findAll(spec, pageable).map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID recipientId) {
        long count = notificationRepository.countByRecipientIdAndInboxStatusAndDeletedFalse(recipientId, InboxStatus.UNREAD);
        return UnreadCountResponse.builder()
                .userId(recipientId)
                .unreadCount(count)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markRead(UUID id, UUID recipientId) {
        Notification notif = notificationRepository.findByIdAndRecipientIdAndDeletedFalse(id, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        if (notif.getInboxStatus() == InboxStatus.UNREAD) {
            notif.setInboxStatus(InboxStatus.READ);
            notif.setReadAt(Instant.now());
            notif = notificationRepository.save(notif);
        }
        return notificationMapper.toResponse(notif);
    }

    @Override
    @Transactional
    public void markAllRead(UUID recipientId) {
        int updated = notificationRepository.markAllReadForRecipient(recipientId, Instant.now());
        log.info("Marked {} notifications read for recipient={}", updated, recipientId);
    }

    private NotificationType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return NotificationType.fromCode(type.trim());
        } catch (Exception ex) {
            log.warn("Ignoring unknown notification type filter '{}'", type);
            return null;
        }
    }
}
