package com.insightflow.notification.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.notification.event.payload.MlRecommendationPayload;
import com.insightflow.notification.repository.ProcessedEventRepository;
import com.insightflow.notification.service.NotificationDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class MlRecommendationConsumer extends BaseEventConsumer {

    private static final Set<String> ACTIONABLE = Set.of("CLEARANCE", "RESTOCK");

    private final NotificationDispatchService dispatchService;

    public MlRecommendationConsumer(ObjectMapper objectMapper,
                                    ProcessedEventRepository processedEventRepository,
                                    NotificationDispatchService dispatchService) {
        super(objectMapper, processedEventRepository);
        this.dispatchService = dispatchService;
    }

    @KafkaListener(topics = "ml.recommendation.created", containerFactory = "kafkaListenerContainerFactory")
    public void onRecommendationCreated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, MlRecommendationPayload.class, "ml.recommendation.created", ack);
    }

    @Override
    protected String extractEventId(Object payload) {
        return payload instanceof MlRecommendationPayload p ? p.getEventId() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handle(Object raw) {
        MlRecommendationPayload p = (MlRecommendationPayload) raw;

        if (p.getTenantId() == null) return;

        // Only alert on HIGH priority actionable recommendations
        if (!"HIGH".equals(p.getPriority()) || !ACTIONABLE.contains(p.getAction())) {
            return;
        }

        String title = switch (p.getAction()) {
            case "CLEARANCE" -> "Action Required: Clearance Recommended";
            case "RESTOCK"   -> "Action Required: Restock Recommended";
            default          -> "ML Recommendation";
        };

        String body = p.getReason() != null ? p.getReason()
                : String.format("%s action suggested for variant %s", p.getAction(), p.getVariantId());

        log.info("HIGH priority recommendation tenant={} action={} variant={}",
                p.getTenantId(), p.getAction(), p.getVariantId());

        dispatchService.dispatch(
                p.getTenantId(), null,
                "RECOMMENDATION", title, body,
                Map.of(
                        "variantId", String.valueOf(p.getVariantId()),
                        "action", p.getAction(),
                        "priority", p.getPriority()
                ));
    }
}
