package com.insightflow.notification.repository;

import com.insightflow.notification.entity.WebSocketSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebSocketSessionRepository extends JpaRepository<WebSocketSession, UUID> {

    Optional<WebSocketSession> findBySessionId(String sessionId);

    List<WebSocketSession> findByUserIdAndActiveTrue(UUID userId);

    long countByActiveTrue();

    boolean existsBySessionId(String sessionId);

    List<WebSocketSession> findByLastHeartbeatAtBefore(Instant cutoff);
}

