package com.insightflow.notification.service.impl;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.enums.DeliveryPolicy;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.NotificationType;
import com.insightflow.notification.service.interfaces.NotificationChannelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Channels are decided by the notification type's {@link DeliveryPolicy}, not by
 * raw severity. Inbox persistence is unconditional (handled by the orchestrator);
 * this only resolves the outbound push channels.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationChannelRouterImpl implements NotificationChannelRouter {

    @Override
    public List<NotificationChannel> resolveChannels(Notification notification) {
        if (notification == null || notification.getNotificationType() == null) {
            return List.of();
        }
        DeliveryPolicy policy = notification.getNotificationType().getDeliveryPolicy();
        return switch (policy) {
            case REALTIME -> List.of(NotificationChannel.WEBSOCKET, NotificationChannel.EMAIL);
            case DAILY_REPORT -> List.of(NotificationChannel.EMAIL);
            case DASHBOARD_NONE -> {
                // Dashboard-only insight should not reach the notification pipeline.
                log.warn("DASHBOARD_NONE notification reached router (type={}, id={}); no channels.",
                        notification.getNotificationType(), notification.getId());
                yield List.of();
            }
        };
    }
}
