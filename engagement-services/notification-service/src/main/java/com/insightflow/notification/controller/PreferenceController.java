package com.insightflow.notification.controller;

import com.insightflow.notification.dto.request.NotificationPreferenceRequest;
import com.insightflow.notification.dto.response.UserNotificationPreferenceResponse;
import com.insightflow.common.web.exception.UnauthorizedException;
import com.insightflow.notification.service.interfaces.NotificationPreferenceService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Notification Preferences", description = "Per-user notification channel preferences")
public class PreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "List notification preferences for the current user")
    public ResponseEntity<List<UserNotificationPreferenceResponse>> getPreferences(@CurrentUser UserContext user) {
        return ResponseEntity.ok(preferenceService.getPreferences(userId(user)));
    }

    @PutMapping
    @Operation(summary = "Upsert a notification preference",
            description = "Enable/disable a (notificationType, channel) pair with a minimum severity. Send a SINGLE JSON object.")
    public ResponseEntity<UserNotificationPreferenceResponse> upsert(
            @CurrentUser UserContext user,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        return ResponseEntity.ok(preferenceService.upsert(userId(user), request));
    }

    private UUID userId(UserContext user) {
        if (user == null || user.userId() == null) {
            throw new UnauthorizedException("Missing user context");
        }
        return user.userId();
    }
}
