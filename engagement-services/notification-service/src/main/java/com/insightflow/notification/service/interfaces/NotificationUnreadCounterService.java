package com.insightflow.notification.service.interfaces;

import java.util.UUID;

public interface NotificationUnreadCounterService {

    long getUnreadCount(UUID recipientId);

    long incrementUnread(UUID recipientId);

    long decrementUnread(UUID recipientId);

    long resetUnread(UUID recipientId);
}

