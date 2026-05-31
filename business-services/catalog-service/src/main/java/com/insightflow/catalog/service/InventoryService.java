package com.insightflow.catalog.service;

import com.insightflow.catalog.dto.request.RecordMovementRequest;
import com.insightflow.catalog.dto.response.InventoryLevelResponse;
import com.insightflow.catalog.dto.response.InventoryMovementResponse;
import com.insightflow.catalog.dto.response.InventorySummaryResponse;
import com.insightflow.catalog.entity.InventoryLevel;
import com.insightflow.catalog.entity.InventoryMovement;
import com.insightflow.catalog.entity.Location;
import com.insightflow.catalog.entity.ProductVariant;
import com.insightflow.common.events.catalog.InventoryUpdatedEvent;
import com.insightflow.common.web.exception.ResourceNotFoundException;
import com.insightflow.catalog.mapper.InventoryMapper;
import com.insightflow.catalog.repository.InventoryLevelRepository;
import com.insightflow.catalog.repository.InventoryMovementRepository;
import com.insightflow.catalog.repository.LocationRepository;
import com.insightflow.catalog.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryLevelRepository levelRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductVariantRepository variantRepository;
    private final LocationRepository locationRepository;
    private final InventoryMapper inventoryMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional(readOnly = true)
    public List<InventoryLevelResponse> getInventoryByVariant(UUID variantId, UUID tenantId) {
        return levelRepository.findByTenantIdAndVariantId(tenantId, variantId).stream()
                .map(inventoryMapper::toLevelResponse)
                .toList();
    }

    @Transactional
    public InventoryMovementResponse recordMovement(RecordMovementRequest request, UUID tenantId) {
        ProductVariant variant = variantRepository.findByTenantIdAndId(tenantId, request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant not found: " + request.getVariantId()));

        Location location = locationRepository.findByTenantIdAndId(tenantId, request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + request.getLocationId()));

        // Upsert inventory level
        InventoryLevel level = levelRepository
                .findByVariantIdAndLocationId(variant.getId(), location.getId())
                .orElseGet(() -> {
                    InventoryLevel newLevel = new InventoryLevel();
                    newLevel.setTenantId(tenantId);
                    newLevel.setVariant(variant);
                    newLevel.setLocation(location);
                    return newLevel;
                });

        level.setQuantityOnHand(level.getQuantityOnHand() + request.getQuantityChange());
        InventoryLevel savedLevel = levelRepository.save(level);

        // Append movement record (never update/delete)
        InventoryMovement movement = new InventoryMovement();
        movement.setTenantId(tenantId);
        movement.setVariantId(variant.getId());
        movement.setLocationId(location.getId());
        movement.setMovementType(request.getMovementType());
        movement.setQuantityChange(request.getQuantityChange());
        movement.setReferenceType(request.getReferenceType());
        movement.setReferenceId(request.getReferenceId());
        movement.setNotes(request.getNotes());
        InventoryMovement saved = movementRepository.save(movement);

        // Publish Kafka event — non-blocking, fail-open
        publishInventoryEvent(tenantId, variant, location.getId(),
                request.getMovementType(), request.getQuantityChange(),
                savedLevel.getQuantityOnHand(), request.getReferenceType(),
                request.getReferenceId());

        return inventoryMapper.toMovementResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> getMovementHistory(
            UUID variantId, UUID tenantId, Pageable pageable) {
        return movementRepository
                .findByTenantIdAndVariantIdOrderByCreatedAtDesc(tenantId, variantId, pageable)
                .map(inventoryMapper::toMovementResponse);
    }

    @Transactional(readOnly = true)
    public InventorySummaryResponse getSummary(UUID tenantId) {
        long totalSKU      = variantRepository.countActiveByTenantId(tenantId);
        long totalQuantity = levelRepository.sumQuantityOnHand(tenantId);
        long lowStockCount = levelRepository.countLowStock(tenantId);
        return new InventorySummaryResponse(totalSKU, totalQuantity, lowStockCount);
    }

    private void publishInventoryEvent(UUID tenantId, ProductVariant variant, UUID locationId,
                                       String movementType, int quantityChange, int quantityOnHand,
                                       String referenceType, UUID referenceId) {
        InventoryUpdatedEvent event = InventoryUpdatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("catalog.inventory.updated")
                .tenantId(tenantId.toString())
                .variantId(variant.getId().toString())
                .locationId(locationId.toString())
                .movementType(movementType)
                .quantityChange(quantityChange)
                .quantityOnHand(quantityOnHand)
                .productId(variant.getProduct().getId().toString())
                .sku(variant.getSku())
                .referenceType(referenceType)
                .referenceId(referenceId != null ? referenceId.toString() : null)
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send("catalog.inventory.updated", tenantId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish InventoryUpdatedEvent variantId={}: {}",
                                variant.getId(), ex.getMessage());
                    } else {
                        log.debug("Published InventoryUpdatedEvent variantId={} offset={}",
                                variant.getId(), result.getRecordMetadata().offset());
                    }
                });
    }
}
