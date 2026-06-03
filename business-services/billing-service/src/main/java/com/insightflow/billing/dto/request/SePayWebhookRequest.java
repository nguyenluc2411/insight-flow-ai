package com.insightflow.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SePayWebhookRequest {
    private String id;
    private String gateway;

    @JsonProperty("transactionDate")
    private String transactionDate;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("senderAccountNumber")
    private String senderAccountNumber;

    private String code;

    private String content;

    @JsonProperty("transferAmount")
    private Integer transferAmount;

    @JsonProperty("accumulated")
    private Integer accumulated;
}