package com.insightflow.billing.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic billingSubscriptionChangedTopic() {
        return TopicBuilder.name("billing.subscription.changed")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic billingSubscriptionCreatedTopic() {
        return TopicBuilder.name("billing.subscription.created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic billingSubscriptionExpiredTopic() {
        return TopicBuilder.name("billing.subscription.expired")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
