package com.insightflow.recommendation.entity;

import com.insightflow.recommendation.enums.RecommendationPriority;
import com.insightflow.recommendation.enums.RecommendationStatus;
import com.insightflow.recommendation.enums.RecommendationType;
import com.insightflow.recommendation.enums.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "recommendations", schema = "recommendation_db")
@Getter
@Setter
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false, length = 30)
    private RecommendationType recommendationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecommendationStatus status = RecommendationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationPriority priority = RecommendationPriority.MEDIUM;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceScore = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> actionPayload = new HashMap<>();

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private int version = 1;
}
