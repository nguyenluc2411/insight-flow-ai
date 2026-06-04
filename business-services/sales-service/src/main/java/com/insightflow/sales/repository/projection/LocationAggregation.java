package com.insightflow.sales.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface LocationAggregation {
    UUID getLocationId();
    Long getOrderCount();
    BigDecimal getTotalRevenue();
}
