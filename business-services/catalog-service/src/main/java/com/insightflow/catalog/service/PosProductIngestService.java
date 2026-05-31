package com.insightflow.catalog.service;

import com.insightflow.catalog.entity.Product;
import com.insightflow.catalog.entity.ProductVariant;
import com.insightflow.catalog.repository.ProductRepository;
import com.insightflow.catalog.repository.ProductVariantRepository;
import com.insightflow.common.events.integration.ProductSyncedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Upserts catalog Products/Variants from POS product-synced events.
 * The POS product code becomes the variant SKU (so POS orders can later be
 * resolved to variants by SKU), and external ids are recorded for traceability.
 * Idempotent — re-syncing updates price/external ids rather than duplicating.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosProductIngestService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    @Transactional
    public void upsert(UUID tenantId, String connectorType, ProductSyncedEvent.SyncedProductPayload p) {
        String sku = p.getSku();
        if (sku == null || sku.isBlank()) {
            log.debug("Skipping POS product without code (tenant={})", tenantId);
            return;
        }

        var existing = variantRepository.findByTenantIdAndSku(tenantId, sku);
        if (existing.isPresent()) {
            ProductVariant v = existing.get();
            if (p.getPrice() != null) {
                v.setSellingPrice(p.getPrice());
            }
            v.setExternalIds(externalIds(connectorType, p.getExternalId(), sku));
            variantRepository.save(v);
            return;
        }

        Product product = productRepository.findByTenantIdAndSkuRoot(tenantId, sku)
                .orElseGet(() -> {
                    Product np = new Product();
                    np.setTenantId(tenantId);
                    np.setSkuRoot(sku);
                    np.setName(p.getName() != null && !p.getName().isBlank() ? p.getName() : sku);
                    np.setStatus("active");
                    np.setSource(connectorType);
                    np.setExternalIds(externalIds(connectorType, p.getExternalId(), sku));
                    return productRepository.save(np);
                });

        ProductVariant variant = new ProductVariant();
        variant.setTenantId(tenantId);
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setSellingPrice(p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
        variant.setStatus("active");
        variant.setExternalIds(externalIds(connectorType, p.getExternalId(), sku));
        variantRepository.save(variant);
        log.debug("Ingested POS variant sku={} tenant={} from {}", sku, tenantId, connectorType);
    }

    private Map<String, Object> externalIds(String connectorType, String externalId, String code) {
        String prefix = connectorType == null ? "pos" : connectorType.toLowerCase();
        Map<String, Object> ids = new HashMap<>();
        if (externalId != null) ids.put(prefix + "_product_id", externalId);
        ids.put(prefix + "_code", code);
        return ids;
    }
}
