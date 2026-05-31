package com.insightflow.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLocationRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Pattern(regexp = "store|warehouse", message = "type must be store or warehouse")
    private String type;

    private String address;

    @Size(max = 100)
    private String city;
}
