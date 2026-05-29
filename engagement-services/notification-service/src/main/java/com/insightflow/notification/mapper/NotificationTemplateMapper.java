package com.insightflow.notification.mapper;

import com.insightflow.notification.dto.request.NotificationTemplateRequest;
import com.insightflow.notification.dto.response.NotificationTemplateResponse;
import com.insightflow.notification.entity.NotificationTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationTemplateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NotificationTemplate toEntity(NotificationTemplateRequest request);

    NotificationTemplateResponse toResponse(NotificationTemplate template);
}
