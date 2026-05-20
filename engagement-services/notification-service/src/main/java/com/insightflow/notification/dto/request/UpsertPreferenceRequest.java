package com.insightflow.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

@Data
@Schema(
    description = "Single notification preference update. Send ONE object, NOT an array.",
    example = "{\"eventType\":\"LOW_STOCK\",\"channel\":\"IN_APP\",\"enabled\":true}"
)
public class UpsertPreferenceRequest {

    @NotBlank
    @Pattern(regexp = "LOW_STOCK|RECOMMENDATION|FORECAST",
             message = "eventType must be LOW_STOCK, RECOMMENDATION, or FORECAST")
    @Schema(description = "Event type to configure", example = "LOW_STOCK",
            allowableValues = {"LOW_STOCK", "RECOMMENDATION", "FORECAST"})
    private String eventType;

    @NotBlank
    @Pattern(regexp = "IN_APP|EMAIL",
             message = "channel must be IN_APP or EMAIL")
    @Schema(description = "Delivery channel", example = "IN_APP",
            allowableValues = {"IN_APP", "EMAIL"})
    private String channel;

    @Schema(description = "Enable or disable this preference", example = "true")
    private boolean enabled = true;

    @Schema(description = "Optional threshold config (e.g. {\"stockLevel\": 10})", nullable = true)
    private Map<String, Object> threshold;
}
