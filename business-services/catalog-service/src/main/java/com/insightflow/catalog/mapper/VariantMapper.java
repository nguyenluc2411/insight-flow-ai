package com.insightflow.catalog.mapper;

import com.insightflow.catalog.dto.response.VariantResponse;
import com.insightflow.catalog.entity.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VariantMapper {

    @Mapping(target = "productId", source = "product.id")
    VariantResponse toResponse(ProductVariant variant);
}
