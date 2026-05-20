package com.insightflow.notification.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.common.events.ml.ForecastGeneratedEvent;
import com.insightflow.notification.repository.ProcessedEventRepository;
import com.insightflow.notification.service.NotificationDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class MlForecastConsumer extends BaseEventConsumer {

    private final NotificationDispatchService dispatchService;

    public MlForecastConsumer(ObjectMapper objectMapper,
                              ProcessedEventRepository processedEventRepository,
                              NotificationDispatchService dispatchService) {
        super(objectMapper, processedEventRepository);
        this.dispatchService = dispatchService;
    }

    @KafkaListener(topics = "ml.forecast.generated", containerFactory = "kafkaListenerContainerFactory")
    public void onForecastGenerated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, ForecastGeneratedEvent.class, "ml.forecast.generated", ack);
    }

    @Override
    protected String extractEventId(Object payload) {
        return payload instanceof ForecastGeneratedEvent p ? p.getEventId() : null;
    }

    @Override
    protected void handle(Object raw) {
        ForecastGeneratedEvent p = (ForecastGeneratedEvent) raw;

        if (p.getTenantId() == null) return;

        String title = "New Forecast Available";
        String body = String.format(
                "%d-day demand forecast generated for variant %s (confidence: %s).",
                p.getForecastHorizon(), p.getVariantId(), p.getConfidence());

        log.info("Forecast generated tenant={} variant={} confidence={}",
                p.getTenantId(), p.getVariantId(), p.getConfidence());

        dispatchService.dispatch(
                UUID.fromString(p.getTenantId()), null,
                "FORECAST", title, body,
                Map.of(
                        "variantId", p.getVariantId(),
                        "forecastHorizon", p.getForecastHorizon(),
                        "confidence", String.valueOf(p.getConfidence())
                ));
    }
}
