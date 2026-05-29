package com.insightflow.notification.service.aggregation;

import com.insightflow.notification.event.incoming.IncomingNotificationEvent;

import java.util.Optional;

public interface AggregationService {

    boolean tryAggregate(IncomingNotificationEvent event);

    Optional<Long> getAggregatedCount(String aggregationKey);
}
