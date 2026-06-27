package com.insightflow.auth.repository;

import com.insightflow.auth.entity.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {
    
    @Query("SELECT COUNT(DISTINCT a.sessionId) FROM AnalyticsEvent a WHERE a.eventType = 'PAGE_VIEW'")
    long countUniqueVisitors();
    
    @Query("SELECT COUNT(DISTINCT a.sessionId) FROM AnalyticsEvent a WHERE a.eventType = 'PAGE_VIEW' AND (CAST(:fromDate AS timestamp) IS NULL OR a.createdAt >= :fromDate) AND (CAST(:toDate AS timestamp) IS NULL OR a.createdAt <= :toDate)")
    long countUniqueVisitorsSince(@org.springframework.data.repository.query.Param("fromDate") java.time.OffsetDateTime fromDate, @org.springframework.data.repository.query.Param("toDate") java.time.OffsetDateTime toDate);
    
    @Query("SELECT COUNT(DISTINCT a.sessionId) FROM AnalyticsEvent a WHERE a.eventType = 'SIGNUP_CLICK' OR a.eventType = 'REGISTER_SUCCESS'")
    long countRegisteredLeads();

    interface SessionStats {
        String getSessionId();
        java.time.OffsetDateTime getFirstAccess();
        java.time.OffsetDateTime getLastAccess();
    }

    @Query("SELECT a.sessionId AS sessionId, MIN(a.createdAt) AS firstAccess, MAX(a.createdAt) AS lastAccess " +
           "FROM AnalyticsEvent a " +
           "WHERE (CAST(:fromDate AS timestamp) IS NULL OR a.createdAt >= :fromDate) " +
           "AND (CAST(:toDate AS timestamp) IS NULL OR a.createdAt <= :toDate) " +
           "GROUP BY a.sessionId " +
           "ORDER BY MAX(a.createdAt) DESC")
    java.util.List<SessionStats> findRecentSessionStats(
            @org.springframework.data.repository.query.Param("fromDate") java.time.OffsetDateTime fromDate, 
            @org.springframework.data.repository.query.Param("toDate") java.time.OffsetDateTime toDate, 
            org.springframework.data.domain.Pageable pageable);

    AnalyticsEvent findFirstBySessionIdOrderByCreatedAtDesc(String sessionId);
}
