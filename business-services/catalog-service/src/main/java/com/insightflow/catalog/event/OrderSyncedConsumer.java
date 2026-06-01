package com.insightflow.catalog.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.catalog.entity.ProductVariant;
import com.insightflow.catalog.repository.ProductVariantRepository;
import com.insightflow.common.events.catalog.CatalogOrderNormalizedEvent;
import com.insightflow.common.events.integration.OrderSyncedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumes integration.order.synced (POS orders), resolves each line's POS product
 * code to an internal catalog variant (by SKU), and re-publishes
 * catalog.order.normalized so ml-service can ingest variant-keyed sales for
 * forecast training. Lines whose product can't be resolved are skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSyncedConsumer {

    private static final String NORMALIZED_TOPIC = "catalog.order.normalized";

    private final ProductVariantRepository variantRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "integration.order.synced", groupId = "catalog-service-events")
    public void handle(String message, Acknowledgment ack) {
        try {
            OrderSyncedEvent event = objectMapper.readValue(message, OrderSyncedEvent.class);
            if (event.getOrders() == null || event.getTenantId() == null) {
                ack.acknowledge();
                return;
            }
            UUID tenantId = UUID.fromString(event.getTenantId());
            int published = 0;
            for (OrderSyncedEvent.SyncedOrderPayload order : event.getOrders()) {
                if (normalizeAndPublish(tenantId, event.getConnectorType(), order)) {
                    published++;
                }
            }
            log.info("Normalized {}/{} POS orders tenant={} from {}",
                    published, event.getOrders().size(), tenantId, event.getConnectorType());
        } catch (Exception e) {
            log.error("Failed to parse integration.order.synced: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    private boolean normalizeAndPublish(UUID tenantId, String connectorType,
                                        OrderSyncedEvent.SyncedOrderPayload order) {
        if (order.getLines() == null || order.getLines().isEmpty()) {
            return false;
        }

        List<CatalogOrderNormalizedEvent.NormalizedLine> items = new ArrayList<>();
        for (OrderSyncedEvent.SyncedOrderLine line : order.getLines()) {
            if (line.getProductCode() == null) continue;
            Optional<ProductVariant> variant =
                    variantRepository.findByTenantIdAndSku(tenantId, line.getProductCode());
            if (variant.isEmpty()) {
                log.debug("Unresolved POS line code={} tenant={} order={} — skipped",
                        line.getProductCode(), tenantId, order.getExternalId());
                continue;
            }
            items.add(CatalogOrderNormalizedEvent.NormalizedLine.builder()
                    .variantId(variant.get().getId().toString())
                    .sku(line.getProductCode())
                    .quantity(line.getQuantity())
                    .unitPrice(line.getUnitPrice())
                    .build());
        }

        if (items.isEmpty()) {
            return false;
        }

        CatalogOrderNormalizedEvent normalized = CatalogOrderNormalizedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("catalog.order.normalized")
                .tenantId(tenantId.toString())
                .occurredAt(Instant.now())
                .connectorType(connectorType)
                .externalOrderId(order.getExternalId())
                .orderCode(order.getOrderCode())
                .orderedAt(order.getOrderedAt())
                .items(items)
                .build();

        kafkaTemplate.send(NORMALIZED_TOPIC, tenantId.toString(), normalized);
        return true;
    }
}
