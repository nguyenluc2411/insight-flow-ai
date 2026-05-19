package com.insightflow.integration.connector.kiotviet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KvProduct {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("categoryName")
    private String categoryName;

    @JsonProperty("retailPrice")
    private BigDecimal retailPrice;

    @JsonProperty("basePrice")
    private BigDecimal basePrice;

    @JsonProperty("images")
    private List<String> images;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("description")
    private String description;

    @JsonProperty("productOptions")
    private List<KvProductOption> productOptions;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KvProductOption {
        @JsonProperty("name")
        private String name;
        @JsonProperty("optionValues")
        private List<String> optionValues;
    }
}
