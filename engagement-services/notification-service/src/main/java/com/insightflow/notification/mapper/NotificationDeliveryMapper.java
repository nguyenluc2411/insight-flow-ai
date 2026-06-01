package com.insightflow.notification.mapper;

import com.insightflow.notification.dto.response.NotificationDeliveryResponse;
import com.insightflow.notification.entity.NotificationDeliveryHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationDeliveryMapper {

    @Mapping(target = "notificationId", source = "notification.id")
    NotificationDeliveryResponse toResponse(NotificationDeliveryHistory history);
}

