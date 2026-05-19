package com.insightflow.notification.service;

import com.insightflow.notification.dto.response.NotificationResponse;
import com.insightflow.notification.dto.response.UnreadCountResponse;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.exception.ResourceNotFoundException;
import com.insightflow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listNotifications(UUID tenantId, Boolean unreadOnly, String type, Pageable pageable) {
        Page<Notification> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findByTenantIdAndIsReadFalseOrderByCreatedAtDesc(tenantId, pageable);
        } else if (type != null && !type.isBlank()) {
            page = notificationRepository.findByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, type.toUpperCase(), pageable);
        } else {
            page = notificationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        }
        return page.map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID tenantId) {
        return new UnreadCountResponse(notificationRepository.countByTenantIdAndIsReadFalse(tenantId));
    }

    @Transactional
    public NotificationResponse markRead(UUID id, UUID tenantId) {
        Notification n = notificationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        n.setRead(true);
        return NotificationResponse.from(notificationRepository.save(n));
    }

    @Transactional
    public int markAllRead(UUID tenantId) {
        return notificationRepository.markAllReadByTenantId(tenantId);
    }
}
