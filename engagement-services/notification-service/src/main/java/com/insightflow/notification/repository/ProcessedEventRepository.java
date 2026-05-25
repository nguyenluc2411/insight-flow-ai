package com.insightflow.notification.repository;

import com.insightflow.notification.entity.ProcessedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    Optional<ProcessedEvent> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    Page<ProcessedEvent> findByEventType(String eventType, Pageable pageable);

    long deleteByProcessedAtBefore(Instant cutoff);
}
