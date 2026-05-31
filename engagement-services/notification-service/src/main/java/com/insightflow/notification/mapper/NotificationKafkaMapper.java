package com.insightflow.notification.mapper;

import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.enums.FailureType;
import com.insightflow.notification.enums.NotificationChannel;
import com.insightflow.common.events.notification.NotificationBroadcastEvent;
import com.insightflow.common.events.notification.NotificationDlqEvent;
import com.insightflow.common.events.notification.NotificationFailedEvent;
import com.insightflow.common.events.notification.NotificationRetryEvent;
import com.insightflow.common.events.notification.NotificationSentEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {Instant.class, UUID.class,
        NotificationSentEvent.class, NotificationFailedEvent.class,
        NotificationRetryEvent.class, NotificationBroadcastEvent.class,
        NotificationDlqEvent.class})
public interface NotificationKafkaMapper {

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "eventType", expression = "java(NotificationSentEvent.TYPE)")
    @Mapping(target = "notificationId", source = "notification.id")
    @Mapping(target = "notificationType", source = "notification.notificationType")
    @Mapping(target = "recipientId", source = "notification.recipientId")
    @Mapping(target = "correlationId", source = "notification.correlationId")
    @Mapping(target = "channel", source = "channel")
    @Mapping(target = "timestamp", expression = "java(Instant.now())")
    NotificationSentEvent toSentEvent(Notification notification, NotificationChannel channel);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "eventType", expression = "java(NotificationFailedEvent.TYPE)")
    @Mapping(target = "notificationId", source = "notification.id")
    @Mapping(target = "notificationType", source = "notification.notificationType")
    @Mapping(target = "recipientId", source = "notification.recipientId")
    @Mapping(target = "correlationId", source = "notification.correlationId")
    @Mapping(target = "channel", source = "channel")
    @Mapping(target = "failureReason", source = "failureReason")
    @Mapping(target = "timestamp", expression = "java(Instant.now())")
    NotificationFailedEvent toFailedEvent(Notification notification, NotificationChannel channel, String failureReason);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "eventType", expression = "java(NotificationRetryEvent.TYPE)")
    @Mapping(target = "notificationId", source = "notification.id")
    @Mapping(target = "channel", source = "channel")
    @Mapping(target = "retryAttempt", source = "retryAttempt")
    @Mapping(target = "nextRetryAt", source = "nextRetryAt")
    @Mapping(target = "failureReason", source = "failureReason")
    @Mapping(target = "correlationId", source = "notification.correlationId")
    @Mapping(target = "timestamp", expression = "java(Instant.now())")
    NotificationRetryEvent toRetryEvent(
            Notification notification,
            NotificationChannel channel,
            int retryAttempt,
            Instant nextRetryAt,
            String failureReason);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "eventType", expression = "java(NotificationBroadcastEvent.TYPE)")
    @Mapping(target = "notificationId", source = "notification.id")
    @Mapping(target = "notificationType", source = "notification.notificationType")
    @Mapping(target = "recipientId", source = "notification.recipientId")
    @Mapping(target = "correlationId", source = "notification.correlationId")
    @Mapping(target = "timestamp", expression = "java(Instant.now())")
    NotificationBroadcastEvent toBroadcastEvent(Notification notification);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "eventType", expression = "java(NotificationDlqEvent.TYPE)")
    @Mapping(target = "notificationId", source = "notification.id")
    @Mapping(target = "notificationType", source = "notification.notificationType")
    @Mapping(target = "recipientId", source = "notification.recipientId")
    @Mapping(target = "correlationId", source = "notification.correlationId")
    @Mapping(target = "channel", source = "channel")
    @Mapping(target = "retryCount", source = "retryCount")
    @Mapping(target = "failureType", source = "failureType")
    @Mapping(target = "failureReason", source = "failureReason")
    @Mapping(target = "timestamp", expression = "java(Instant.now())")
    NotificationDlqEvent toDlqEvent(
            Notification notification,
            NotificationChannel channel,
            int retryCount,
            FailureType failureType,
            String failureReason);
}

