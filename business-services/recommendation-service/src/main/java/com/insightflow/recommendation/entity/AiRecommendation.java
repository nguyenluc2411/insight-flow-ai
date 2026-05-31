package com.insightflow.recommendation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "ai_recommendations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AiRecommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // 👉 THÊM DÒNG NÀY VÀO
    private String id;

    @Column(name = "workspace_id", length = 36, nullable = false, unique = true)
    private String workspaceId;

    // Lưu trữ nguyên cục JSON trả về từ AI (có thể rất dài nên dùng LONGTEXT)
    @Column(name = "recommendation_json", columnDefinition = "LONGTEXT")
    private String recommendationJson;

    // Trạng thái chạy AI: PENDING, PROCESSING, COMPLETED, FAILED
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;
}