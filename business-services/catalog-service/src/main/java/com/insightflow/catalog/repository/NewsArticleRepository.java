package com.insightflow.catalog.repository;

import com.insightflow.catalog.entity.NewsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {
    Page<NewsArticle> findByTenantId(UUID tenantId, Pageable pageable);
    Page<NewsArticle> findByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);
    Page<NewsArticle> findByStatus(String status, Pageable pageable);
    Optional<NewsArticle> findByIdAndTenantId(UUID id, UUID tenantId);
}
