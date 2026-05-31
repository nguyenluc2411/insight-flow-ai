package com.insightflow.notification.service.retry;

import com.insightflow.notification.config.kafka.NotificationKafkaTopics;
import com.insightflow.notification.exception.BusinessException;
import com.insightflow.notification.exception.NotificationDeliveryException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RetryTopicRoutingServiceImpl implements RetryTopicRoutingService {

    @Override
    public String resolveDestination(String sourceTopic, Exception exception) {
        if (!isRetryable(exception)) {
            return NotificationKafkaTopics.DLQ;
        }

        if (NotificationKafkaTopics.RETRY_30S.equals(sourceTopic)) {
            return NotificationKafkaTopics.RETRY_2M;
        }

        if (NotificationKafkaTopics.RETRY_2M.equals(sourceTopic)) {
            return NotificationKafkaTopics.RETRY_10M;
        }

        if (NotificationKafkaTopics.RETRY_10M.equals(sourceTopic)) {
            return NotificationKafkaTopics.DLQ;
        }

        return NotificationKafkaTopics.RETRY_30S;
    }

    @Override
    public int resolveAttempt(String sourceTopic) {
        if (NotificationKafkaTopics.RETRY_30S.equals(sourceTopic)) {
            return 1;
        }
        if (NotificationKafkaTopics.RETRY_2M.equals(sourceTopic)) {
            return 2;
        }
        if (NotificationKafkaTopics.RETRY_10M.equals(sourceTopic)) {
            return 3;
        }
        return 0;
    }

    @Override
    public Duration resolveDelay(String sourceTopic) {
        if (NotificationKafkaTopics.RETRY_30S.equals(sourceTopic)
                || NotificationKafkaTopics.PRIORITY_TOPICS.contains(sourceTopic)) {
            return Duration.ofSeconds(30);
        }
        if (NotificationKafkaTopics.RETRY_2M.equals(sourceTopic)) {
            return Duration.ofMinutes(2);
        }
        if (NotificationKafkaTopics.RETRY_10M.equals(sourceTopic)) {
            return Duration.ofMinutes(10);
        }
        return Duration.ZERO;
    }

    private boolean isRetryable(Exception exception) {
        if (exception instanceof NotificationDeliveryException) {
            return true;
        }
        if (exception instanceof BusinessException) {
            return false;
        }
        return !(exception instanceof IllegalArgumentException);
    }
}

