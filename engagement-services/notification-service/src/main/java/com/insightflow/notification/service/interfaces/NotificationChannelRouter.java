package com.insightflow.notification.service.interfaces;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.enums.NotificationChannel;

import java.util.List;

public interface NotificationChannelRouter {

    List<NotificationChannel> resolveChannels(Notification notification);
}
