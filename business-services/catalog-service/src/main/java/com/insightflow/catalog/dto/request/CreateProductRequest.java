package com.insightflow.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateProductRequest {

    @NotBlank
    @Size(max = 100)
    private String skuRoot;

    @NotBlank
    @Size(max = 500)
    private String name;

    private String description;
    private UUID categoryId;

    @Size(max = 255)
    private String brand;

    @Size(max = 50)
    private String season;

    @Size(max = 20)
    private String gender;

    private List<String> tags;
}
