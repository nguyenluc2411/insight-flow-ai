package com.insightflow.notification.config.kafka;

import com.insightflow.common.events.notification.IncomingNotificationEvent;
import com.insightflow.common.events.notification.NotificationCreatedEvent;
import com.insightflow.common.events.notification.NotificationRetryEvent;
import com.insightflow.notification.service.retry.RetryTopicRoutingService;
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
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@EnableKafka
@Configuration
public class KafkaListenerConfig {

    @Bean
    public DefaultErrorHandler notificationErrorHandler(
            KafkaTemplate<String, Object> notificationKafkaTemplate,
            RetryTopicRoutingService retryTopicRoutingService) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                notificationKafkaTemplate,
                (record, ex) -> {
                    String destination = retryTopicRoutingService.resolveDestination(record.topic(), ex);
                    return new TopicPartition(destination, record.partition());
                });

        return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IncomingNotificationEvent>
    incomingNotificationKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties,
            DefaultErrorHandler notificationErrorHandler) {

        return buildListenerFactory(kafkaProperties, notificationErrorHandler, IncomingNotificationEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationRetryEvent>
    retryKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties,
            DefaultErrorHandler notificationErrorHandler) {

        return buildListenerFactory(kafkaProperties, notificationErrorHandler, NotificationRetryEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    dlqKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties,
            DefaultErrorHandler notificationErrorHandler) {

        return buildListenerFactory(kafkaProperties, notificationErrorHandler, Object.class);
    }

    @Bean
    public DefaultErrorHandler notificationCreatedErrorHandler(
            KafkaTemplate<String, Object> notificationKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                notificationKafkaTemplate,
                (record, ex) -> new TopicPartition(NotificationKafkaTopics.OUTGOING_DLQ, record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationCreatedEvent>
    notificationCreatedKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties,
            DefaultErrorHandler notificationCreatedErrorHandler) {

        return buildListenerFactory(kafkaProperties, notificationCreatedErrorHandler, NotificationCreatedEvent.class);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> buildListenerFactory(
            KafkaProperties kafkaProperties,
            DefaultErrorHandler notificationErrorHandler,
            Class<T> targetType) {

        Map<String, Object> consumerProps = kafkaProperties.buildConsumerProperties(null);
        consumerProps.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        JsonDeserializer<T> valueDeserializer = new JsonDeserializer<>(targetType, false);
        valueDeserializer.addTrustedPackages("com.insightflow.common.events.notification");
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
        factory.setCommonErrorHandler(notificationErrorHandler);

        return factory;
    }
}

