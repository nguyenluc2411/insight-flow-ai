package com.insightflow.auth.repository;

import com.insightflow.auth.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** Super-admin tenant search — excludes the internal platform tenant. */
    @Query("""
        SELECT t FROM Tenant t
        WHERE t.slug <> :excludeSlug
          AND (:status IS NULL OR t.status = :status)
          AND (:q IS NULL
               OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(t.slug) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Tenant> searchTenants(@Param("excludeSlug") String excludeSlug,
                               @Param("status") String status,
                               @Param("q") String q,
                               Pageable pageable);
}
