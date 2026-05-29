package com.insightflow.notification.repository;

import com.insightflow.notification.entity.NotificationAggregationWindow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationAggregationWindowRepository extends JpaRepository<NotificationAggregationWindow, UUID> {

    Optional<NotificationAggregationWindow> findFirstByAggregationKeyAndActiveTrue(String aggregationKey);

    Page<NotificationAggregationWindow> findByWindowStartBetween(Instant start, Instant end, Pageable pageable);

    long countByActiveTrue();

    boolean existsByAggregationKeyAndActiveTrue(String aggregationKey);
}
