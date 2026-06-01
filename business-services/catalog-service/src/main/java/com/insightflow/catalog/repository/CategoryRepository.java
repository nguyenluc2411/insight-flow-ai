package com.insightflow.catalog.repository;

import com.insightflow.catalog.dto.response.CategorySummaryItem;
import com.insightflow.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Category> findByTenantIdAndSlug(UUID tenantId, String slug);

    /**
     * Flat list of categories with active product count — single query, no N+1.
     * JPQL constructor expression maps directly to CategorySummaryItem record.
     * COUNT(p.id) returns 0 for categories with no active products (LEFT JOIN).
     */
    @Query("""
            SELECT new com.insightflow.catalog.dto.response.CategorySummaryItem(
                c.id, c.name, COUNT(p.id)
            )
            FROM Category c
            LEFT JOIN Product p
                ON p.category = c
                AND p.tenantId = :tenantId
                AND p.status  = 'active'
            WHERE c.tenantId = :tenantId
            GROUP BY c.id, c.name
            ORDER BY c.name ASC
            """)
    List<CategorySummaryItem> findSummariesByTenantId(UUID tenantId);
}
