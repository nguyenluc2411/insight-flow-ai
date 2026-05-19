package com.insightflow.integration.connector.kiotviet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KvOrder {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("code")
    private String code;

    @JsonProperty("purchaseDate")
    private Instant purchaseDate;

    @JsonProperty("total")
    private BigDecimal total;

    @JsonProperty("discount")
    private BigDecimal discount;

    @JsonProperty("branchId")
    private Long branchId;

    @JsonProperty("branchName")
    private String branchName;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("status")
    private String status;

    @JsonProperty("customer")
    private KvCustomer customer;

    @JsonProperty("orderDetails")
    private List<KvOrderDetail> orderDetails;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KvCustomer {
        @JsonProperty("id")
        private Long id;
        @JsonProperty("code")
        private String code;
        @JsonProperty("name")
        private String name;
        @JsonProperty("contactNumber")
        private String contactNumber;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KvOrderDetail {
        @JsonProperty("productId")
        private Long productId;
        @JsonProperty("productCode")
        private String productCode;
        @JsonProperty("productName")
        private String productName;
        @JsonProperty("quantity")
        private Integer quantity;
        @JsonProperty("price")
        private BigDecimal price;
        @JsonProperty("discount")
        private BigDecimal discount;
    }
}
