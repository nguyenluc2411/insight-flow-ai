package com.insightflow.integration.connector.kiotviet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KvInventory {

    @JsonProperty("productId")
    private Long productId;

    @JsonProperty("productCode")
    private String productCode;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("branchId")
    private Long branchId;

    @JsonProperty("branchName")
    private String branchName;

    @JsonProperty("onHand")
    private Double onHand;

    @JsonProperty("reserved")
    private Double reserved;

    @JsonProperty("minQuantity")
    private Double minQuantity;

    @JsonProperty("maxQuantity")
    private Double maxQuantity;
}
