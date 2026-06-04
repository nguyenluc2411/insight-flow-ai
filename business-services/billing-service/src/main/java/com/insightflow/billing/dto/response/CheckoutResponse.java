package com.insightflow.billing.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutResponse {
    private String qrUrl;
    private Integer amount;
    private String content;
    private String bankId;
    private String accountNo;
    private String accountName;
}