package com.insightflow.notification.service.interfaces;

import com.insightflow.notification.dto.request.NotificationInboxFilterRequest;
import com.insightflow.notification.dto.response.NotificationResponse;
import com.insightflow.notification.dto.response.UnreadCountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationInboxService {

    Page<NotificationResponse> getInbox(UUID recipientId, NotificationInboxFilterRequest filter, Pageable pageable);

    UnreadCountResponse getUnreadCount(UUID recipientId);

    NotificationResponse markAsRead(UUID id, UUID recipientId);

    UnreadCountResponse markAllAsRead(UUID recipientId);

    NotificationResponse archive(UUID id, UUID recipientId);

    NotificationResponse delete(UUID id, UUID recipientId);
}
