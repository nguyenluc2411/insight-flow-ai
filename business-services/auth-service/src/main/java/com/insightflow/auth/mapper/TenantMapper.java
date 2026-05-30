package com.insightflow.auth.mapper;

import com.insightflow.auth.dto.response.TenantResponse;
import com.insightflow.auth.entity.Tenant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantResponse toResponse(Tenant tenant);
}
