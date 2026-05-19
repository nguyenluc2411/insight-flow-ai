package com.insightflow.catalog.service;

import com.insightflow.catalog.dto.request.RecordMovementRequest;
import com.insightflow.catalog.dto.response.InventoryLevelResponse;
import com.insightflow.catalog.dto.response.InventoryMovementResponse;
import com.insightflow.catalog.dto.response.InventorySummaryResponse;
import com.insightflow.catalog.entity.InventoryLevel;
import com.insightflow.catalog.entity.InventoryMovement;
import com.insightflow.catalog.entity.Location;
import com.insightflow.catalog.entity.ProductVariant;
import com.insightflow.catalog.event.InventoryUpdatedEvent;
import com.insightflow.catalog.exception.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", request.getVariantId()));

        Location location = locationRepository.findByTenantIdAndId(tenantId, request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", request.getLocationId()));

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
        publishInventoryEvent(tenantId, variant.getId(), location.getId(),
                request.getMovementType(), request.getQuantityChange(),
                savedLevel.getQuantityOnHand());

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

    private void publishInventoryEvent(UUID tenantId, UUID variantId, UUID locationId,
                                       String movementType, int quantityChange, int newQty) {
        InventoryUpdatedEvent event = InventoryUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(InventoryUpdatedEvent.TYPE)
                .tenantId(tenantId)
                .variantId(variantId)
                .locationId(locationId)
                .movementType(movementType)
                .quantityChange(quantityChange)
                .newQuantityOnHand(newQty)
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send(InventoryUpdatedEvent.TOPIC, tenantId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish InventoryUpdatedEvent variantId={}: {}",
                                variantId, ex.getMessage());
                    } else {
                        log.debug("Published InventoryUpdatedEvent variantId={} offset={}",
                                variantId, result.getRecordMetadata().offset());
                    }
                });
    }
}
