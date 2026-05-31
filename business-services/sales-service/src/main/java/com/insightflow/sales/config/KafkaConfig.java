package com.insightflow.sales.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic orderCompletedTopic() {
        return TopicBuilder.name("sales.order.completed")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
