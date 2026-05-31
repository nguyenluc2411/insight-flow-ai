package com.insightflow.recommendation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Entity
@Table(name = "recommendation_histories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(name = "status")
    private String status; // PROCESSING, DONE, ERROR

    // ĐÃ SỬA THÀNH LONGTEXT ĐỂ KHÔNG BAO GIỜ BỊ TRÀN DATA
    @Column(name = "recommendation_result", columnDefinition = "LONGTEXT")
    private String recommendationResult;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}