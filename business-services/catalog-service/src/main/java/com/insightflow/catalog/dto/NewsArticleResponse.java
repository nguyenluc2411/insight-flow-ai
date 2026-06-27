package com.insightflow.catalog.dto;

import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
public class NewsArticleResponse {
    private UUID id;
    private String title;
    private String summary;
    private Map<String, Object> content;
    private String coverImageUrl;
    private UUID authorId;
    private String status;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Double averageRating;
    private Long ratingCount;
}
