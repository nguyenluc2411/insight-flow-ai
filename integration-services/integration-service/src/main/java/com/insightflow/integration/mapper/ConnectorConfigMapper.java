package com.insightflow.integration.mapper;

import com.insightflow.integration.dto.response.ConnectorConfigResponse;
import com.insightflow.integration.entity.ConnectorConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConnectorConfigMapper {

    ConnectorConfigResponse toResponse(ConnectorConfig entity);
}
