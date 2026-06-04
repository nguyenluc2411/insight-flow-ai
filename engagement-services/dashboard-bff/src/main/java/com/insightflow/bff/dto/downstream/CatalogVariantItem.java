package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogVariantItem {

    private UUID id;
    private String sku;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("productId")
    private UUID productId;
}
