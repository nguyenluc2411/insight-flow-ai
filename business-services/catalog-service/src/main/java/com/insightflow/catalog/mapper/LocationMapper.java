package com.insightflow.catalog.mapper;

import com.insightflow.catalog.dto.response.LocationResponse;
import com.insightflow.catalog.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "active", source = "active")
    LocationResponse toResponse(Location location);
}
