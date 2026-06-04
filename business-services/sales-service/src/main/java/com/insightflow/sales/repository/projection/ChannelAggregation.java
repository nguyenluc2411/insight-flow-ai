package com.insightflow.sales.repository.projection;

import java.math.BigDecimal;

public interface ChannelAggregation {
    String getChannel();
    Long getOrderCount();
    BigDecimal getTotalRevenue();
}
