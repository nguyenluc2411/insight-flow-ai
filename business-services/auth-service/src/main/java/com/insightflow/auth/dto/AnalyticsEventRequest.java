package com.insightflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalyticsEventRequest {
    @NotBlank(message = "Session ID is required")
    private String sessionId;
    
    @NotBlank(message = "Event type is required")
    private String eventType;
    
    private String url;
    private String utmSource;
    private java.util.UUID tenantId;
    private Object metadata;
}
