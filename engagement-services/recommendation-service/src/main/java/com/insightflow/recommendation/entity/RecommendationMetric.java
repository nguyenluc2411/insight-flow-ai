package com.insightflow.recommendation.entity;

import com.insightflow.recommendation.enums.RecommendationType;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "recommendation_metrics", schema = "recommendation_db")
@Getter
@Setter
public class RecommendationMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false, length = 30)
    private RecommendationType recommendationType;

    @Column(name = "generated_count", nullable = false)
    private int generatedCount;

    @Column(name = "applied_count", nullable = false)
    private int appliedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "avg_confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal avgConfidenceScore = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
