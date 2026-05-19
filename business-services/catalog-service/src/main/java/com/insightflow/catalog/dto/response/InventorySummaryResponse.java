package com.insightflow.catalog.dto.response;

/**
 * High-level inventory snapshot for the dashboard.
 * totalSKU:      active variants.
 * totalQuantity: sum of quantity_on_hand across all locations.
 * lowStockCount: stock positions (variant × location) at or below reorder threshold.
 */
public record InventorySummaryResponse(long totalSKU, long totalQuantity, long lowStockCount) {}
