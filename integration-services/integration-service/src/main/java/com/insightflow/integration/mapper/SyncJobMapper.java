package com.insightflow.integration.mapper;

import com.insightflow.integration.dto.response.SyncJobResponse;
import com.insightflow.integration.entity.SyncJob;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SyncJobMapper {

    SyncJobResponse toResponse(SyncJob entity);
}
