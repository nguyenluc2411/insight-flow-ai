package com.insightflow.billing.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RateLimitResponse {

    private boolean allowed;
    private int remaining;
    private int limit;
    private int rateLimitPerMinute;
}
