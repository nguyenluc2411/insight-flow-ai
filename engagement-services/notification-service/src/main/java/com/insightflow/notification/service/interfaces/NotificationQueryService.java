package com.insightflow.notification.service.interfaces;

import com.insightflow.notification.dto.response.NotificationResponse;
import com.insightflow.notification.dto.response.UnreadCountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Read-side inbox API: list, count and mark notifications for the current
 * recipient. recipientId is the gateway-scoped tenant id (notifications are
 * created with recipientId = tenantId by the domain-event bridge).
 */
public interface NotificationQueryService {

    Page<NotificationResponse> listNotifications(UUID recipientId, Boolean unreadOnly, String type, Pageable pageable);

    UnreadCountResponse getUnreadCount(UUID recipientId);

    NotificationResponse markRead(UUID id, UUID recipientId);

    void markAllRead(UUID recipientId);
}
