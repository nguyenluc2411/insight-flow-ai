package com.insightflow.integration.event.producer;

import com.insightflow.common.events.integration.InventorySyncedEvent;
import com.insightflow.common.events.integration.OrderSyncedEvent;
import com.insightflow.common.events.integration.ProductSyncedEvent;
import com.insightflow.common.events.integration.SyncCompletedEvent;
import com.insightflow.integration.connector.kiotviet.model.KvInventory;
import com.insightflow.integration.connector.kiotviet.model.KvOrder;
import com.insightflow.integration.connector.kiotviet.model.KvProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishProductSynced(UUID tenantId, UUID connectorConfigId,
                                     UUID syncJobId, String connectorType,
                                     List<KvProduct> products) {
        List<ProductSyncedEvent.SyncedProductPayload> payloads = products.stream()
                .map(p -> ProductSyncedEvent.SyncedProductPayload.builder()
                        .externalId(p.getId() != null ? p.getId().toString() : null)
                        .name(p.getName())
                        .sku(p.getCode())
                        .price(p.getRetailPrice())
                        .stockQuantity(0)
                        .categoryName(p.getCategoryName())
                        .build())
                .toList();

        ProductSyncedEvent event = ProductSyncedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("integration.product.synced")
                .tenantId(tenantId.toString())
                .occurredAt(Instant.now())
                .connectorType(connectorType)
                .connectorConfigId(connectorConfigId.toString())
                .syncJobId(syncJobId.toString())
                .products(payloads)
                .build();

        publish("integration.product.synced", tenantId.toString(), event);
    }

    public void publishOrderSynced(UUID tenantId, UUID connectorConfigId,
                                   UUID syncJobId, String connectorType,
                                   List<KvOrder> orders) {
        List<OrderSyncedEvent.SyncedOrderPayload> payloads = orders.stream()
                .map(o -> OrderSyncedEvent.SyncedOrderPayload.builder()
                        .externalId(o.getId() != null ? o.getId().toString() : null)
                        .orderCode(o.getCode())
                        .customerName(o.getCustomer() != null ? o.getCustomer().getName() : null)
                        .totalAmount(o.getTotal())
                        .status(o.getStatus())
                        .orderedAt(o.getPurchaseDate())
                        .lines(mapOrderLines(o))
                        .build())
                .toList();

        OrderSyncedEvent event = OrderSyncedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("integration.order.synced")
                .tenantId(tenantId.toString())
                .occurredAt(Instant.now())
                .connectorType(connectorType)
                .connectorConfigId(connectorConfigId.toString())
                .syncJobId(syncJobId.toString())
                .orders(payloads)
                .build();

        publish("integration.order.synced", tenantId.toString(), event);
    }

    private List<OrderSyncedEvent.SyncedOrderLine> mapOrderLines(KvOrder o) {
        if (o.getOrderDetails() == null) {
            return List.of();
        }
        return o.getOrderDetails().stream()
                .map(d -> OrderSyncedEvent.SyncedOrderLine.builder()
                        .externalProductId(d.getProductId() != null ? d.getProductId().toString() : null)
                        .productCode(d.getProductCode())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getPrice())
                        .build())
                .toList();
    }

    public void publishInventorySynced(UUID tenantId, UUID connectorConfigId,
                                       UUID syncJobId, String connectorType,
                                       List<KvInventory> inventoryItems) {
        List<InventorySyncedEvent.SyncedInventoryPayload> payloads = inventoryItems.stream()
                .map(i -> InventorySyncedEvent.SyncedInventoryPayload.builder()
                        .externalProductId(i.getProductId() != null ? i.getProductId().toString() : null)
                        .sku(i.getProductCode())
                        .branchId(i.getBranchId() != null ? i.getBranchId().toString() : null)
                        .quantity(i.getOnHand() != null ? (int) i.getOnHand().doubleValue() : 0)
                        .build())
                .toList();

        InventorySyncedEvent event = InventorySyncedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("integration.inventory.synced")
                .tenantId(tenantId.toString())
                .occurredAt(Instant.now())
                .connectorType(connectorType)
                .connectorConfigId(connectorConfigId.toString())
                .syncJobId(syncJobId.toString())
                .inventoryItems(payloads)
                .build();

        publish("integration.inventory.synced", tenantId.toString(), event);
    }

    public void publishSyncCompleted(UUID tenantId, UUID connectorConfigId,
                                     UUID syncJobId, String connectorType, String syncType,
                                     String status, int totalProducts, int totalOrders,
                                     int totalInventory, long durationMs, String errorMessage) {
        SyncCompletedEvent event = SyncCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("integration.sync.completed")
                .tenantId(tenantId.toString())
                .occurredAt(Instant.now())
                .syncJobId(syncJobId.toString())
                .connectorType(connectorType)
                .connectorConfigId(connectorConfigId.toString())
                .syncType(syncType)
                .status(status)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalInventory(totalInventory)
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .build();

        publish("integration.sync.completed", tenantId.toString(), event);
    }

    private void publish(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish Kafka event to topic={}: {}", topic, ex.getMessage());
                    } else {
                        log.debug("Kafka event published topic={} partition={} offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
