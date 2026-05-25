package com.insightflow.notification.producer;

public interface KafkaEventPublisher {

    <T> void publish(String topic, String key, T event);
}
