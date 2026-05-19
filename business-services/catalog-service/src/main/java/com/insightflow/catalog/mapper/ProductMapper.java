package com.insightflow.catalog.mapper;

import com.insightflow.catalog.dto.response.ProductResponse;
import com.insightflow.catalog.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    ProductResponse toResponse(Product product);
}
