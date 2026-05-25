package com.insightflow.notification.mapper;

import com.insightflow.notification.dto.kafka.NotificationBroadcastEventDto;
import com.insightflow.notification.dto.kafka.NotificationFailedEventDto;
import com.insightflow.notification.dto.kafka.NotificationRetryEventDto;
import com.insightflow.notification.dto.kafka.NotificationSentEventDto;
import com.insightflow.notification.entity.Notification;
import com.insightflow.notification.enums.NotificationChannel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

@Mapper(componentModel = "spring", imports = Instant.class)
public interface NotificationKafkaMapper {

    @Mapping(target = "eventId", source = "notification.eventId")
    @Mapping(target = "notificationId", source = "notification.id")
    @Mapping(target = "notificationType", source = "notification.notificationType")
    @Mapping(target = "recipientId", source = "notification.recipientId")
    @Mapping(target = "correlationId", source = "notification.correlationId")
    @Mapping(target = "channel", source = "channel")
    @Mapping(target = "timestamp", expression = "java(Instant.now())")
    NotificationSentEventDto toSentEvent(Notification notification, NotificationChannel channel);

    @Mapping(target = "eventId", source = "notification.eventId")
    @Mapping(target = "notificationId", source = "notification.id")
    @Mapping(target = "notificationType", source = "notification.notificationType")
    @Mapping(target = "recipientId", source = "notification.recipientId")
    @Mapping(target = "correlationId", source = "notification.correlationId")
    @Mapping(target = "channel", source = "channel")
    @Mapping(target = "failureReason", source = "failureReason")
    @Mapping(target = "timestamp", expression = "java(Instant.now())")
    NotificationFailedEventDto toFailedEvent(Notification notification, NotificationChannel channel, String failureReason);

    @Mapping(target = "eventId", source = "notification.eventId")
    @Mapping(target = "notificationId", source = "notification.id")
    @Mapping(target = "channel", source = "channel")
    @Mapping(target = "retryAttempt", source = "retryAttempt")
    @Mapping(target = "nextRetryAt", source = "nextRetryAt")
    @Mapping(target = "failureReason", source = "failureReason")
    @Mapping(target = "correlationId", source = "notification.correlationId")
    @Mapping(target = "timestamp", expression = "java(Instant.now())")
    NotificationRetryEventDto toRetryEvent(
            Notification notification,
            NotificationChannel channel,
            int retryAttempt,
            Instant nextRetryAt,
            String failureReason);

    @Mapping(target = "eventId", source = "notification.eventId")
    @Mapping(target = "notificationId", source = "notification.id")
    @Mapping(target = "notificationType", source = "notification.notificationType")
    @Mapping(target = "recipientId", source = "notification.recipientId")
    @Mapping(target = "correlationId", source = "notification.correlationId")
    @Mapping(target = "timestamp", expression = "java(Instant.now())")
    NotificationBroadcastEventDto toBroadcastEvent(Notification notification);
}
