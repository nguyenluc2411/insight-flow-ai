package com.insightflow.notification.mapper;

import com.insightflow.notification.websocket.payload.RealtimeNotificationPayload;
import com.insightflow.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationWebSocketMapper {

    @Mapping(target = "notificationId", source = "id")
    @Mapping(target = "type", source = "notificationType")
    RealtimeNotificationPayload toPayload(Notification notification);
}
