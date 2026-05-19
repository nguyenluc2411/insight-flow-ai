package com.insightflow.notification.controller;

import com.insightflow.notification.dto.request.UpsertPreferenceRequest;
import com.insightflow.notification.dto.response.PreferenceResponse;
import com.insightflow.notification.service.PreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Per-tenant notification channel preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Get notification preferences for tenant")
    @ApiResponse(responseCode = "200", description = "Preference list")
    public ResponseEntity<List<PreferenceResponse>> getPreferences(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(preferenceService.getPreferences(tenantId));
    }

    @PutMapping
    @Operation(summary = "Upsert notification preference",
               description = "Enable or disable a channel (EMAIL|IN_APP) for a given event type (LOW_STOCK|RECOMMENDATION|FORECAST).")
    @ApiResponse(responseCode = "200", description = "Saved preference")
    public ResponseEntity<PreferenceResponse> upsert(
            @Parameter(hidden = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody UpsertPreferenceRequest request) {
        return ResponseEntity.ok(preferenceService.upsert(tenantId, userId, request));
    }
}
