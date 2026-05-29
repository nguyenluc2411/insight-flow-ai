package com.insightflow.catalog.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic inventoryUpdatedTopic() {
        return TopicBuilder.name("catalog.inventory.updated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCompletedTopic() {
        return TopicBuilder.name("sales.order.completed")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Pre-create so consumers (ml-service) discover it at startup rather than
    // waiting for a metadata refresh after first publish.
    @Bean
    public NewTopic orderNormalizedTopic() {
        return TopicBuilder.name("catalog.order.normalized")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
