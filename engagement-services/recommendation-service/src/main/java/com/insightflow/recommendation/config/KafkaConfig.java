package com.insightflow.recommendation.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic recommendationGeneratedTopic() {
        return TopicBuilder.name("recommendation.generated.v1")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic clearanceRecommendationTopic() {
        return TopicBuilder.name("recommendation.clearance.v1")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic restockRecommendationTopic() {
        return TopicBuilder.name("recommendation.restock.v1")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
