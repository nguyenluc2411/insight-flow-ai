package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

/** Single product item from catalog-service */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogProductItem {

    private UUID id;
    private String name;
    private String status;

    @JsonProperty("categoryId")
    private UUID categoryId;
}
