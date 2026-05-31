package com.insightflow.notification.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private static final int DEFAULT_PARTITIONS = 3;
    private static final int DEFAULT_REPLICAS = 1;

    @Bean
    public NewTopic notificationsHighTopic() {
        return TopicBuilder.name(NotificationKafkaTopics.HIGH_PRIORITY)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationsNormalTopic() {
        return TopicBuilder.name(NotificationKafkaTopics.NORMAL_PRIORITY)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationsLowTopic() {
        return TopicBuilder.name(NotificationKafkaTopics.LOW_PRIORITY)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationsRetry30sTopic() {
        return TopicBuilder.name(NotificationKafkaTopics.RETRY_30S)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationsRetry2mTopic() {
        return TopicBuilder.name(NotificationKafkaTopics.RETRY_2M)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationsRetry10mTopic() {
        return TopicBuilder.name(NotificationKafkaTopics.RETRY_10M)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationsDlqTopic() {
        return TopicBuilder.name(NotificationKafkaTopics.DLQ)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    // Outgoing lifecycle topics
    @Bean
    public NewTopic notificationsSentTopic() {
        return TopicBuilder.name("notifications.outgoing.sent")
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationsFailedTopic() {
        return TopicBuilder.name("notifications.outgoing.failed")
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationsRetryTopic() {
        return TopicBuilder.name("notifications.outgoing.retry")
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationsBroadcastTopic() {
        return TopicBuilder.name("notifications.outgoing.broadcast")
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationsCreatedTopic() {
        return TopicBuilder.name(NotificationKafkaTopics.OUTGOING_NOTIFICATION_EVENT)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationsOutgoingDlqTopic() {
        return TopicBuilder.name(NotificationKafkaTopics.OUTGOING_DLQ)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }
}


