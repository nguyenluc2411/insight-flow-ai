package com.insightflow.notification.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.notification.entity.ProcessedEvent;
import com.insightflow.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseEventConsumer {

    protected final ObjectMapper objectMapper;
    protected final ProcessedEventRepository processedEventRepository;

    /**
     * Template method: parse → dedup → handle → ack.
     * Catches all exceptions so the consumer loop never dies.
     */
    protected <T> void process(ConsumerRecord<String, String> record,
                                Class<T> payloadType,
                                String topic,
                                Acknowledgment ack) {
        try {
            T payload = objectMapper.readValue(record.value(), payloadType);
            String eventId = extractEventId(payload);

            if (eventId == null || eventId.isBlank()) {
                log.warn("Event on topic {} has no eventId — processing anyway", topic);
            } else if (processedEventRepository.existsById(eventId)) {
                log.debug("Duplicate event {} on topic {} — skipping", eventId, topic);
                ack.acknowledge();
                return;
            }

            handle(payload);

            if (eventId != null && !eventId.isBlank()) {
                processedEventRepository.save(new ProcessedEvent(eventId, topic));
            }
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Failed to process event on topic {} offset {}: {}",
                    topic, record.offset(), ex.getMessage(), ex);
            // Acknowledge to avoid infinite redelivery of a poison pill message
            ack.acknowledge();
        }
    }

    protected abstract String extractEventId(Object payload);

    protected abstract void handle(Object payload);
}
