package com.insightflow.catalog.dto;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class NewsArticleRequest {
    private String title;
    private String summary;
    private Map<String, Object> content;
    private String coverImageUrl;
    private String status;
}
