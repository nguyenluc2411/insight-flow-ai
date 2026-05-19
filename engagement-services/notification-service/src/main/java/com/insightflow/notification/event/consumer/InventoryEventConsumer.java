package com.insightflow.notification.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.notification.event.payload.InventoryUpdatedPayload;
import com.insightflow.notification.repository.ProcessedEventRepository;
import com.insightflow.notification.service.NotificationDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryEventConsumer extends BaseEventConsumer {

    private final NotificationDispatchService dispatchService;

    @Value("${app.notification.default-low-stock-threshold:10}")
    private int defaultLowStockThreshold;

    public InventoryEventConsumer(ObjectMapper objectMapper,
                                  ProcessedEventRepository processedEventRepository,
                                  NotificationDispatchService dispatchService) {
        super(objectMapper, processedEventRepository);
        this.dispatchService = dispatchService;
    }

    @KafkaListener(topics = "catalog.inventory.updated", containerFactory = "kafkaListenerContainerFactory")
    public void onInventoryUpdated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, InventoryUpdatedPayload.class, "catalog.inventory.updated", ack);
    }

    @Override
    protected String extractEventId(Object payload) {
        return payload instanceof InventoryUpdatedPayload p ? p.getEventId() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handle(Object raw) {
        InventoryUpdatedPayload p = (InventoryUpdatedPayload) raw;

        if (p.getTenantId() == null) {
            log.warn("InventoryUpdated event missing tenantId — skipping");
            return;
        }

        if (p.getNewQuantityOnHand() > defaultLowStockThreshold) {
            return; // Not low stock — nothing to notify
        }

        String title = "Low Stock Alert";
        String body = String.format(
                "Variant %s is running low: %d units remaining.",
                p.getVariantId(), p.getNewQuantityOnHand());

        log.info("Low stock detected tenant={} variant={} qty={}",
                p.getTenantId(), p.getVariantId(), p.getNewQuantityOnHand());

        dispatchService.dispatch(
                p.getTenantId(), null,
                "LOW_STOCK", title, body,
                java.util.Map.of(
                        "variantId", String.valueOf(p.getVariantId()),
                        "newQuantityOnHand", p.getNewQuantityOnHand(),
                        "movementType", String.valueOf(p.getMovementType())
                ));
    }
}
