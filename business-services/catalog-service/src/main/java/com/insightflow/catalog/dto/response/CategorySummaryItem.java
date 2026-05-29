package com.insightflow.catalog.dto.response;

import java.util.UUID;

/**
 * Flat category entry with active product count.
 * Used as JPQL constructor expression target — field order must match the query.
 */
public record CategorySummaryItem(UUID id, String name, long productCount) {}
