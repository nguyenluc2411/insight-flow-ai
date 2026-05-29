package com.insightflow.billing.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeatureAccessResponse {

    private String featureCode;
    private boolean hasAccess;
    private String reason;
}
