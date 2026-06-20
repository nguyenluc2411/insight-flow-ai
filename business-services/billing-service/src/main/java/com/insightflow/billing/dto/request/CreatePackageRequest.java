package com.insightflow.billing.dto.request;

import lombok.Data;

/** Super-admin: create a new package together with an initial monthly plan. */
@Data
public class CreatePackageRequest {
    private String code;
    private String name;
    private String description;
    private Integer displayOrder;
    private Integer monthlyPriceVnd;
    private Integer trialDays;
}
