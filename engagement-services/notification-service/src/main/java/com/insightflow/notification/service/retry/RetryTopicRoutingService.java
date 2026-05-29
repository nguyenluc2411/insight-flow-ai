package com.insightflow.notification.service.retry;

import java.time.Duration;

public interface RetryTopicRoutingService {

    String resolveDestination(String sourceTopic, Exception exception);

    int resolveAttempt(String sourceTopic);

    Duration resolveDelay(String sourceTopic);
}
