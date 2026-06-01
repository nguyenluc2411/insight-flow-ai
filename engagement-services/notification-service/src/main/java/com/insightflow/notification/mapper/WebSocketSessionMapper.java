package com.insightflow.notification.mapper;

import com.insightflow.notification.dto.request.WebSocketSessionRequest;
import com.insightflow.notification.dto.response.WebSocketSessionResponse;
import com.insightflow.notification.entity.WebSocketSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WebSocketSessionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "connectedAt", ignore = true)
    @Mapping(target = "disconnectedAt", ignore = true)
    @Mapping(target = "lastHeartbeatAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WebSocketSession toEntity(WebSocketSessionRequest request);

    WebSocketSessionResponse toResponse(WebSocketSession session);
}

