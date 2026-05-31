package com.insightflow.sales.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCustomerRequest {

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String fullName;

    @Size(max = 20)
    private String gender;
}
