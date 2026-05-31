package com.insightflow.sales.mapper;

import com.insightflow.sales.dto.response.SupplierResponse;
import com.insightflow.sales.entity.Supplier;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    SupplierResponse toResponse(Supplier supplier);
}
