package com.insightflow.auth.repository;

import com.insightflow.auth.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // ── Platform admin metrics (all exclude the internal platform tenant) ──────

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.slug <> :excludeSlug")
    long countNonPlatform(@Param("excludeSlug") String excludeSlug);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.slug <> :excludeSlug AND (CAST(:fromDate AS timestamp) IS NULL OR t.createdAt >= :fromDate) AND (CAST(:toDate AS timestamp) IS NULL OR t.createdAt <= :toDate)")
    long countNonPlatformSince(@Param("excludeSlug") String excludeSlug, @Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.slug <> :excludeSlug AND LOWER(t.plan) NOT IN ('trial', 'free') AND (CAST(:fromDate AS timestamp) IS NULL OR t.createdAt >= :fromDate) AND (CAST(:toDate AS timestamp) IS NULL OR t.createdAt <= :toDate)")
    long countPaidCustomersSince(@Param("excludeSlug") String excludeSlug, @Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);

    /** Returns rows of [status, count]. */
    @Query("SELECT t.status, COUNT(t) FROM Tenant t WHERE t.slug <> :excludeSlug GROUP BY t.status")
    List<Object[]> countByStatus(@Param("excludeSlug") String excludeSlug);

    /** Returns rows of [plan, count]. */
    @Query("SELECT t.plan, COUNT(t) FROM Tenant t WHERE t.slug <> :excludeSlug GROUP BY t.plan")
    List<Object[]> countByPlan(@Param("excludeSlug") String excludeSlug);

    /** Creation timestamps since a cutoff, for bucketing sign-ups by day. */
    @Query("SELECT t.createdAt FROM Tenant t WHERE t.slug <> :excludeSlug AND t.createdAt >= :since")
    List<Instant> findCreatedAtSince(@Param("excludeSlug") String excludeSlug, @Param("since") Instant since);

    /**
     * Super-admin tenant search — excludes the internal platform tenant.
     *
     * <p>{@code :q}/{@code :status} are explicitly cast to text. Without the cast,
     * a null bind has no inferable type and PostgreSQL falls back to {@code bytea},
     * so {@code LOWER(CONCAT(..., :q, ...))} fails with
     * {@code function lower(bytea) does not exist} (500 on the admin tenant list).</p>
     */
    @Query("""
        SELECT t FROM Tenant t
        WHERE t.slug <> :excludeSlug
          AND (CAST(:status AS string) IS NULL OR t.status = :status)
          AND (CAST(:q AS string) IS NULL
               OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(t.slug) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
        """)
    Page<Tenant> searchTenants(@Param("excludeSlug") String excludeSlug,
                               @Param("status") String status,
                               @Param("q") String q,
                               Pageable pageable);
}
