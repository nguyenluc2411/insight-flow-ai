package com.insightflow.catalog.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.catalog.service.PosProductIngestService;
import com.insightflow.common.events.integration.ProductSyncedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes integration.product.synced and upserts catalog products/variants so
 * POS-synced products exist locally (with external-id mapping) and POS orders
 * can later be resolved to variants.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSyncedConsumer {

    private final PosProductIngestService ingestService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "integration.product.synced", groupId = "catalog-service-events")
    public void handle(String message, Acknowledgment ack) {
        try {
            ProductSyncedEvent event = objectMapper.readValue(message, ProductSyncedEvent.class);
            if (event.getProducts() == null || event.getTenantId() == null) {
                ack.acknowledge();
                return;
            }
            UUID tenantId = UUID.fromString(event.getTenantId());
            int ok = 0;
            for (ProductSyncedEvent.SyncedProductPayload p : event.getProducts()) {
                try {
                    ingestService.upsert(tenantId, event.getConnectorType(), p);
                    ok++;
                } catch (Exception e) {
                    log.warn("Failed to ingest POS product sku={} tenant={}: {}",
                            p.getSku(), tenantId, e.getMessage());
                }
            }
            log.info("Ingested {}/{} POS products tenant={} from {}",
                    ok, event.getProducts().size(), tenantId, event.getConnectorType());
        } catch (Exception e) {
            log.error("Failed to parse integration.product.synced: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
