package com.insightflow.catalog.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.catalog.dto.request.RecordMovementRequest;
import com.insightflow.catalog.entity.InventoryLevel;
import com.insightflow.catalog.repository.InventoryLevelRepository;
import com.insightflow.catalog.repository.InventoryMovementRepository;
import com.insightflow.catalog.service.InventoryService;
import com.insightflow.common.events.sales.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Consumes sales.order.completed events and auto-deducts inventory for each order item.
 *
 * Idempotency: keyed on (tenant_id, referenceType="ORDER", referenceId=orderId, variantId).
 * Location selection: deducts from the location holding the most stock for that variant.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompletedConsumer {

    private final InventoryService inventoryService;
    private final InventoryMovementRepository movementRepository;
    private final InventoryLevelRepository levelRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "sales.order.completed", groupId = "catalog-service-events")
    public void handleOrderCompleted(String message, Acknowledgment ack) {
        OrderCompletedEvent event;
        try {
            event = objectMapper.readValue(message, OrderCompletedEvent.class);
        } catch (Exception e) {
            log.warn("Failed to parse sales.order.completed message: {}", e.getMessage());
            ack.acknowledge();
            return;
        }

        UUID tenantId = UUID.fromString(event.getTenantId());
        UUID orderId  = UUID.fromString(event.getOrderId());

        log.info("Processing order completed event orderId={} tenant={} items={}",
                orderId, tenantId, event.getItems().size());

        for (OrderCompletedEvent.OrderItemPayload item : event.getItems()) {
            UUID variantId = UUID.fromString(item.getVariantId());
            processItem(tenantId, orderId, event.getOrderNumber(), variantId, item.getQuantity());
        }

        ack.acknowledge();
    }

    private void processItem(UUID tenantId, UUID orderId, String orderNumber,
                              UUID variantId, int quantity) {
        // Idempotency: skip if this order item was already deducted
        if (movementRepository.existsByTenantIdAndReferenceTypeAndReferenceIdAndVariantId(
                tenantId, "ORDER", orderId, variantId)) {
            log.debug("Skipping duplicate deduction orderId={} variantId={}", orderId, variantId);
            return;
        }

        UUID locationId = findBestLocation(tenantId, variantId);
        if (locationId == null) {
            log.warn("No inventory location for tenant={} variant={} — cannot deduct for order={}",
                    tenantId, variantId, orderId);
            return;
        }

        RecordMovementRequest request = new RecordMovementRequest();
        request.setVariantId(variantId);
        request.setLocationId(locationId);
        request.setMovementType("SALE");
        request.setQuantityChange(-quantity);   // negative = stock out
        request.setReferenceType("ORDER");
        request.setReferenceId(orderId);
        request.setNotes("Auto-deducted from order " + orderNumber);

        try {
            inventoryService.recordMovement(request, tenantId);
            log.info("Deducted {} units variant={} location={} order={}",
                    quantity, variantId, locationId, orderId);
        } catch (Exception e) {
            log.error("Failed to deduct inventory variant={} order={}: {}",
                    variantId, orderId, e.getMessage(), e);
        }
    }

    /**
     * Returns the location holding the most stock for this variant.
     * Choosing the fullest location minimises the chance of going negative.
     * Returns null if no inventory record exists yet for this variant.
     */
    private UUID findBestLocation(UUID tenantId, UUID variantId) {
        List<InventoryLevel> levels = levelRepository.findByTenantIdAndVariantId(tenantId, variantId);
        return levels.stream()
                .max(Comparator.comparingInt(InventoryLevel::getQuantityOnHand))
                .map(l -> l.getLocation().getId())
                .orElse(null);
    }
}
