package com.insightflow.notification.controller;

import com.insightflow.notification.dto.response.NotificationResponse;
import com.insightflow.notification.dto.response.UnreadCountResponse;
import com.insightflow.notification.service.NotificationService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification feed")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List notifications",
               description = "Paginated notification feed for the tenant. Filter by unreadOnly=true or type (LOW_STOCK|RECOMMENDATION|FORECAST).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification list"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Page<NotificationResponse>> list(
            @CurrentUser UserContext user,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.listNotifications(user.tenantId(), unreadOnly, type, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread notification count")
    @ApiResponse(responseCode = "200", description = "Count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@CurrentUser UserContext user) {
        return ResponseEntity.ok(notificationService.getUnreadCount(user.tenantId()));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<NotificationResponse> markRead(
            @CurrentUser UserContext user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markRead(id, user.tenantId()));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    @ApiResponse(responseCode = "200", description = "Updated count")
    public ResponseEntity<Void> markAllRead(@CurrentUser UserContext user) {
        notificationService.markAllRead(user.tenantId());
        return ResponseEntity.ok().build();
    }
}
