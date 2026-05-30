package com.insightflow.notification.service.impl;

import com.insightflow.notification.enums.InboxStatus;
import com.insightflow.notification.repository.NotificationRepository;
import com.insightflow.notification.service.interfaces.NotificationUnreadCounterService;
import com.insightflow.notification.service.redis.UnreadCountCacheService;
import com.insightflow.notification.service.websocket.RealtimeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationUnreadCounterServiceImpl implements NotificationUnreadCounterService {

    private final UnreadCountCacheService unreadCountCacheService;
    private final NotificationRepository notificationRepository;
    private final RealtimeNotificationService realtimeNotificationService;

    @Override
    public long getUnreadCount(UUID recipientId) {
        long count = unreadCountCacheService.getUnreadCount(recipientId);
        log.info("Unread count fetched recipientId={} unreadCount={}", recipientId, count);
        return count;
    }

    @Override
    public long incrementUnread(UUID recipientId) {
        long count = unreadCountCacheService.incrementUnread(recipientId);
        realtimeNotificationService.pushUnreadCount(recipientId);
        log.info("Unread count increment recipientId={} unreadCount={}", recipientId, count);
        return count;
    }

    @Override
    public long decrementUnread(UUID recipientId) {
        long count = unreadCountCacheService.decrementUnread(recipientId);
        realtimeNotificationService.pushUnreadCount(recipientId);
        log.info("Unread count decrement recipientId={} unreadCount={}", recipientId, count);
        return count;
    }

    @Override
    public long resetUnread(UUID recipientId) {
        long count = notificationRepository.countByRecipientIdAndInboxStatus(recipientId, InboxStatus.UNREAD);
        unreadCountCacheService.setUnreadCount(recipientId, count);
        realtimeNotificationService.pushUnreadCount(recipientId);
        log.info("Unread count reset recipientId={} unreadCount={}", recipientId, count);
        return count;
    }
}
