package com.insightflow.notification.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.notification.event.payload.MlForecastPayload;
import com.insightflow.notification.repository.ProcessedEventRepository;
import com.insightflow.notification.service.NotificationDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

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
        process(record, MlForecastPayload.class, "ml.forecast.generated", ack);
    }

    @Override
    protected String extractEventId(Object payload) {
        return payload instanceof MlForecastPayload p ? p.getEventId() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handle(Object raw) {
        MlForecastPayload p = (MlForecastPayload) raw;

        if (p.getTenantId() == null) return;

        String title = "New Forecast Available";
        String body = String.format(
                "%d-day demand forecast generated for variant %s (confidence: %s).",
                p.getForecastDays(), p.getVariantId(), p.getConfidence());

        log.info("Forecast generated tenant={} variant={} confidence={}",
                p.getTenantId(), p.getVariantId(), p.getConfidence());

        dispatchService.dispatch(
                p.getTenantId(), null,
                "FORECAST", title, body,
                Map.of(
                        "variantId", String.valueOf(p.getVariantId()),
                        "forecastDays", p.getForecastDays(),
                        "confidence", String.valueOf(p.getConfidence())
                ));
    }
}
