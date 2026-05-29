package com.insightflow.notification.mapper;

import com.insightflow.notification.dto.kafka.IncomingNotificationEventDto;
import com.insightflow.notification.dto.request.CreateNotificationRequest;
import com.insightflow.notification.dto.response.NotificationResponse;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.enums.NotificationType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {NotificationType.class})
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "inboxStatus", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "aggregationWindow", ignore = true)
    Notification toEntity(CreateNotificationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "inboxStatus", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "aggregationWindow", ignore = true)
    @Mapping(target = "aggregationKey", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "notificationType", expression = "java(NotificationType.fromCode(event.getEventType()))")
    Notification fromIncomingEvent(IncomingNotificationEventDto event);

    NotificationResponse toResponse(Notification notification);
}
