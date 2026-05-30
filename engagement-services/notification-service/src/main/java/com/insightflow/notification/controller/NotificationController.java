package com.insightflow.notification.controller;

import com.insightflow.notification.dto.request.NotificationInboxFilterRequest;
import com.insightflow.notification.dto.response.ApiResponse;
import com.insightflow.notification.dto.response.NotificationResponse;
import com.insightflow.notification.dto.response.UnreadCountResponse;
import com.insightflow.notification.service.interfaces.NotificationInboxService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
@Slf4j
public class NotificationController {

    private final NotificationInboxService notificationInboxService;

    @GetMapping("/inbox")
    public ApiResponse<Page<NotificationResponse>> getInbox(
            @RequestHeader("X-User-Id") @NotNull UUID recipientId,
            @Valid @ModelAttribute NotificationInboxFilterRequest filter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationResponse> response = notificationInboxService.getInbox(recipientId, filter, pageable);
        log.info("Inbox query recipientId={} page={} size={}", recipientId, page, size);
        return ApiResponse.success(response, "Inbox retrieved");
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @RequestHeader("X-User-Id") @NotNull UUID recipientId) {
        UnreadCountResponse response = notificationInboxService.getUnreadCount(recipientId);
        log.info("Unread count query recipientId={} unreadCount={}", recipientId, response.getUnreadCount());
        return ApiResponse.success(response, "Unread count retrieved");
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @RequestHeader("X-User-Id") @NotNull UUID recipientId,
            @PathVariable UUID id) {
        NotificationResponse response = notificationInboxService.markAsRead(id, recipientId);
        return ApiResponse.success(response, "Notification marked as read");
    }

    @PatchMapping("/read-all")
    public ApiResponse<UnreadCountResponse> markAllAsRead(
            @RequestHeader("X-User-Id") @NotNull UUID recipientId) {
        UnreadCountResponse response = notificationInboxService.markAllAsRead(recipientId);
        return ApiResponse.success(response, "All notifications marked as read");
    }

    @PatchMapping("/{id}/archive")
    public ApiResponse<NotificationResponse> archive(
            @RequestHeader("X-User-Id") @NotNull UUID recipientId,
            @PathVariable UUID id) {
        NotificationResponse response = notificationInboxService.archive(id, recipientId);
        return ApiResponse.success(response, "Notification archived");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<NotificationResponse> delete(
            @RequestHeader("X-User-Id") @NotNull UUID recipientId,
            @PathVariable UUID id) {
        NotificationResponse response = notificationInboxService.delete(id, recipientId);
        return ApiResponse.success(response, "Notification deleted");
    }
}
