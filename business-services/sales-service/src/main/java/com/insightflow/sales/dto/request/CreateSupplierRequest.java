package com.insightflow.sales.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSupplierRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String contactName;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String email;

    private String address;
}
