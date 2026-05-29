package com.insightflow.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "websocket_sessions",
        schema = "notification_db",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_websocket_session_id", columnNames = "session_id")
        },
        indexes = {
                @Index(name = "idx_websocket_user_id", columnList = "user_id"),
                @Index(name = "idx_websocket_active", columnList = "active"),
                @Index(name = "idx_websocket_last_heartbeat", columnList = "last_heartbeat_at"),
                @Index(name = "idx_websocket_connected_at", columnList = "connected_at")
        }
)
@Getter
@Setter
public class WebSocketSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "node_id", length = 100)
    private String nodeId;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt = Instant.now();

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
