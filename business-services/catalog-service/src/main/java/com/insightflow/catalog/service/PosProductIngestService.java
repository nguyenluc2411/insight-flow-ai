package com.insightflow.catalog.service;

import com.insightflow.catalog.entity.Category;
import com.insightflow.catalog.entity.Product;
import com.insightflow.catalog.entity.ProductVariant;
import com.insightflow.catalog.repository.CategoryRepository;
import com.insightflow.catalog.repository.ProductRepository;
import com.insightflow.catalog.repository.ProductVariantRepository;
import com.insightflow.common.events.integration.ProductSyncedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

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
    private final CategoryRepository categoryRepository;

    @Transactional
    public void upsert(UUID tenantId, String connectorType, ProductSyncedEvent.SyncedProductPayload p) {
        String sku = p.getSku();
        if (sku == null || sku.isBlank()) {
            log.debug("Skipping POS product without code (tenant={})", tenantId);
            return;
        }

        Category category = resolveCategory(tenantId, p.getCategoryName());

        var existing = variantRepository.findByTenantIdAndSku(tenantId, sku);
        if (existing.isPresent()) {
            ProductVariant v = existing.get();
            if (p.getPrice() != null) {
                v.setSellingPrice(p.getPrice());
            }
            v.setExternalIds(externalIds(connectorType, p.getExternalId(), sku));
            variantRepository.save(v);
            // Backfill category on the parent product if it was missing.
            if (category != null && v.getProduct() != null && v.getProduct().getCategory() == null) {
                v.getProduct().setCategory(category);
                productRepository.save(v.getProduct());
            }
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
                    return np;
                });
        // Assign category on create, or backfill if an existing product had none.
        if (category != null && product.getCategory() == null) {
            product.setCategory(category);
        }
        product = productRepository.save(product);

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

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Finds (or creates) a tenant-scoped category by name. Categories are deduped by
     * slug so "Áo thun" and "ao thun" map to the same category. Returns null when no
     * category name is provided.
     */
    private Category resolveCategory(UUID tenantId, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        String name = categoryName.trim();
        String slug = slugify(name);
        if (slug.isEmpty()) {
            return null;
        }
        return categoryRepository.findByTenantIdAndSlug(tenantId, slug)
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setTenantId(tenantId);
                    c.setName(name);
                    c.setSlug(slug);
                    c.setLevel(1);
                    return categoryRepository.save(c);
                });
    }

    /** "Áo Thun Nam" -> "ao-thun-nam" (diacritics stripped, max 100 chars for the slug column). */
    private String slugify(String input) {
        String temp = Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD);
        temp = DIACRITICS.matcher(temp).replaceAll("");
        temp = temp.replace("đ", "d");
        temp = temp.replaceAll("[^a-z0-9\\s-]", "").trim();
        temp = temp.replaceAll("[\\s-]+", "-");
        return temp.length() > 100 ? temp.substring(0, 100) : temp;
    }

    private Map<String, Object> externalIds(String connectorType, String externalId, String code) {
        String prefix = connectorType == null ? "pos" : connectorType.toLowerCase();
        Map<String, Object> ids = new HashMap<>();
        if (externalId != null) ids.put(prefix + "_product_id", externalId);
        ids.put(prefix + "_code", code);
        return ids;
    }
}
