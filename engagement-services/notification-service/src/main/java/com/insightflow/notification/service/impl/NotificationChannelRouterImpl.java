package com.insightflow.notification.service.impl;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.notification.enums.NotificationSeverity;
import com.insightflow.notification.repository.UserNotificationPreferenceRepository;
import com.insightflow.notification.service.interfaces.NotificationChannelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationChannelRouterImpl implements NotificationChannelRouter {

    private final UserNotificationPreferenceRepository preferenceRepository;

    @Override
    public List<NotificationChannel> resolveChannels(Notification notification) {
        List<NotificationChannel> channels = new ArrayList<>();
        if (notification == null) return channels;

        // Basic severity-based routing
        NotificationSeverity sev = notification.getSeverity();
        if (sev == NotificationSeverity.HIGH || sev == NotificationSeverity.CRITICAL) {
            channels.add(NotificationChannel.WEBSOCKET);
            channels.add(NotificationChannel.EMAIL);
        } else if (sev == NotificationSeverity.MEDIUM) {
            channels.add(NotificationChannel.WEBSOCKET);
            // email for medium only if user opt-in
            // simplified: always include websocket
        } else {
            channels.add(NotificationChannel.WEBSOCKET);
        }

        // TODO: check user preferences and filter channels
        return channels;
    }
}
