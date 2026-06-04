package com.insightflow.integration.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One validated sales row parsed from an uploaded file. A row is only produced
 * when the four required fields (sku, name, quantitySold, saleDate) are present
 * and parseable; optional fields may be null/zero.
 */
public record ImportRowDto(
        String sku,
        String name,
        String categoryName,   // nullable
        int quantitySold,
        BigDecimal unitPrice,  // nullable
        int stockQuantity,
        Instant saleDate
) {
}
