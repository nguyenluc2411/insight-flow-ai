package com.insightflow.notification.controller;

import com.insightflow.notification.dto.response.NotificationResponse;
import com.insightflow.notification.dto.response.UnreadCountResponse;
import com.insightflow.common.web.exception.UnauthorizedException;
import com.insightflow.notification.service.interfaces.NotificationQueryService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Notifications", description = "In-app notification inbox")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping
    @Operation(summary = "List notifications",
            description = "Paginated inbox for the current tenant. Filter by unreadOnly=true or type (a NotificationType code).")
    public ResponseEntity<Page<NotificationResponse>> list(
            @CurrentUser UserContext user,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                notificationQueryService.listNotifications(recipientId(user), unreadOnly, type, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread notification count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@CurrentUser UserContext user) {
        return ResponseEntity.ok(notificationQueryService.getUnreadCount(recipientId(user)));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<NotificationResponse> markRead(
            @CurrentUser UserContext user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(notificationQueryService.markRead(id, recipientId(user)));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllRead(@CurrentUser UserContext user) {
        notificationQueryService.markAllRead(recipientId(user));
        return ResponseEntity.noContent().build();
    }

    private UUID recipientId(UserContext user) {
        if (user == null || user.tenantId() == null) {
            throw new UnauthorizedException("Missing tenant context");
        }
        return user.tenantId();
    }
}
