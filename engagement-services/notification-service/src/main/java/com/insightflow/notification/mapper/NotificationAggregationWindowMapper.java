package com.insightflow.notification.mapper;

import com.insightflow.notification.dto.response.NotificationAggregationWindowResponse;
import com.insightflow.notification.entity.NotificationAggregationWindow;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationAggregationWindowMapper {

    NotificationAggregationWindowResponse toResponse(NotificationAggregationWindow window);
}

