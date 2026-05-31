package com.insightflow.sales.mapper;

import com.insightflow.sales.dto.response.OrderItemResponse;
import com.insightflow.sales.dto.response.SalesOrderResponse;
import com.insightflow.sales.entity.SalesOrder;
import com.insightflow.sales.entity.SalesOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SalesOrderMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "items",      source = "items")
    SalesOrderResponse toResponse(SalesOrder order);

    @Mapping(target = "id", source = "id")
    OrderItemResponse toItemResponse(SalesOrderItem item);
}
