package com.insightflow.notification.mapper;

import com.insightflow.notification.dto.request.NotificationPreferenceRequest;
import com.insightflow.notification.dto.response.UserNotificationPreferenceResponse;
import com.insightflow.notification.entity.UserNotificationPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserNotificationPreferenceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserNotificationPreference toEntity(NotificationPreferenceRequest request);

    UserNotificationPreferenceResponse toResponse(UserNotificationPreference preference);
}
