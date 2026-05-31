package com.insightflow.sales.mapper;

import com.insightflow.sales.dto.response.CustomerResponse;
import com.insightflow.sales.entity.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);
}
