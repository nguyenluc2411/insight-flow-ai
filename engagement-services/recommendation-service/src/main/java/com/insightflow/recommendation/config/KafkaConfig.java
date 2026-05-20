package com.insightflow.recommendation.config;

import com.insightflow.recommendation.event.*;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition())
        );

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(recoverer, new FixedBackOff(5000L, 3));

        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TrendDetectedEvent> trendDetectedKafkaListenerContainerFactory(
            KafkaProperties properties,
            DefaultErrorHandler kafkaErrorHandler) {

        return buildListenerFactory(properties, kafkaErrorHandler, TrendDetectedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryRiskDetectedEvent> inventoryRiskKafkaListenerContainerFactory(
            KafkaProperties properties,
            DefaultErrorHandler kafkaErrorHandler) {

        return buildListenerFactory(properties, kafkaErrorHandler, InventoryRiskDetectedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SalesAnalyticsEvent> salesAnalyticsKafkaListenerContainerFactory(
            KafkaProperties properties,
            DefaultErrorHandler kafkaErrorHandler) {

        return buildListenerFactory(properties, kafkaErrorHandler, SalesAnalyticsEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DemandForecastEvent> demandForecastKafkaListenerContainerFactory(
            KafkaProperties properties,
            DefaultErrorHandler kafkaErrorHandler) {

        return buildListenerFactory(properties, kafkaErrorHandler, DemandForecastEvent.class);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> buildListenerFactory(
            KafkaProperties properties,
            DefaultErrorHandler kafkaErrorHandler,
            Class<T> targetType) {

        Map<String, Object> consumerProps = properties.buildConsumerProperties(null);

        // ❌ REMOVE auto-config conflict
        consumerProps.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        JsonDeserializer<T> valueDeserializer = new JsonDeserializer<>(targetType, false);

        valueDeserializer.addTrustedPackages("com.insightflow.recommendation.event");
        valueDeserializer.setUseTypeMapperForKey(false);

        DefaultKafkaConsumerFactory<String, T> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        consumerProps,
                        new StringDeserializer(),
                        valueDeserializer
                );

        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);

        return factory;
    }

    // =========================
    // Topics
    // =========================

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

    @Bean
    public NewTopic trendDetectedDlqTopic() {
        return TopicBuilder.name("fashion.trend.detected.v1.dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryRiskDlqTopic() {
        return TopicBuilder.name("inventory.risk.detected.v1.dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic salesAnalyticsDlqTopic() {
        return TopicBuilder.name("sales.analytics.generated.v1.dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic demandForecastDlqTopic() {
        return TopicBuilder.name("forecast.demand.generated.v1.dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic recommendationGeneratedDlqTopic() {
        return TopicBuilder.name("recommendation.generated.v1.dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic clearanceRecommendationDlqTopic() {
        return TopicBuilder.name("recommendation.clearance.v1.dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic restockRecommendationDlqTopic() {
        return TopicBuilder.name("recommendation.restock.v1.dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }
}