package com.insightflow.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

@Data
public class UpsertPreferenceRequest {

    @NotBlank
    @Pattern(regexp = "LOW_STOCK|RECOMMENDATION|FORECAST",
             message = "eventType must be LOW_STOCK, RECOMMENDATION, or FORECAST")
    private String eventType;

    @NotBlank
    @Pattern(regexp = "IN_APP|EMAIL",
             message = "channel must be IN_APP or EMAIL")
    private String channel;

    private boolean enabled = true;

    private Map<String, Object> threshold;
}
