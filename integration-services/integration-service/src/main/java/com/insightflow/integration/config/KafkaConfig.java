package com.insightflow.integration.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic integrationProductSyncedTopic() {
        return TopicBuilder.name("integration.product.synced").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic integrationOrderSyncedTopic() {
        return TopicBuilder.name("integration.order.synced").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic integrationInventorySyncedTopic() {
        return TopicBuilder.name("integration.inventory.synced").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic integrationSyncCompletedTopic() {
        return TopicBuilder.name("integration.sync.completed").partitions(3).replicas(1).build();
    }
}
