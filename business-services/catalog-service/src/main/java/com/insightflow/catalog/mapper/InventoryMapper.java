package com.insightflow.catalog.mapper;

import com.insightflow.catalog.dto.response.InventoryLevelResponse;
import com.insightflow.catalog.dto.response.InventoryMovementResponse;
import com.insightflow.catalog.entity.InventoryLevel;
import com.insightflow.catalog.entity.InventoryMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "variantId",         source = "variant.id")
    @Mapping(target = "locationId",        source = "location.id")
    @Mapping(target = "locationName",      source = "location.name")
    @Mapping(target = "quantityAvailable",
             expression = "java(level.getQuantityOnHand() - level.getQuantityReserved())")
    InventoryLevelResponse toLevelResponse(InventoryLevel level);

    InventoryMovementResponse toMovementResponse(InventoryMovement movement);
}
