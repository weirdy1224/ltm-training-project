package com.examly.springapp.controller;

import com.examly.springapp.model.Notification;
import com.examly.springapp.model.User;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Validated
@Tag(name = "Notifications", description = "User notification endpoints")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final AccessService access;

    public NotificationController(NotificationService notificationService, AccessService access) {
        this.notificationService = notificationService;
        this.access = access;
    }

    @Operation(summary = "Get current user notifications", description = "Returns all notifications for the authenticated user, newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications() {
        User user = access.currentUser();
        log.trace("getNotifications called for user {}", user.getUserId());
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user.getUserId()));
    }

    @Operation(summary = "Get unread notifications count", description = "Returns count of unread notifications for the current user.")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        User user = access.currentUser();
        long count = notificationService.getUnreadCount(user.getUserId());
        return ResponseEntity.ok(Collections.singletonMap("count", count));
    }

    @Operation(summary = "Mark a notification as read", description = "Marks a specific notification as read for the current user.")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable @Min(1) Long id) {
        User user = access.currentUser();
        log.trace("markAsRead called for id={}, user={}", id, user.getUserId());
        return ResponseEntity.ok(notificationService.markAsRead(id, user.getUserId()));
    }

    @Operation(summary = "Mark a notification as read (PUT fallback)", description = "Fallback PUT endpoint.")
    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsReadPut(@PathVariable @Min(1) Long id) {
        return markAsRead(id);
    }

    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications for the current user as read.")
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        User user = access.currentUser();
        log.trace("markAllAsRead called for user {}", user.getUserId());
        notificationService.markAllAsRead(user.getUserId());
        return ResponseEntity.ok(Collections.singletonMap("message", "All notifications marked as read"));
    }

    @Operation(summary = "Mark all notifications as read (PUT fallback)", description = "Fallback PUT endpoint.")
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsReadPut() {
        return markAllAsRead();
    }

    @Operation(summary = "Delete a notification", description = "Deletes a specific notification for the current user.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable @Min(1) Long id) {
        User user = access.currentUser();
        log.trace("deleteNotification called for id={}, user={}", id, user.getUserId());
        notificationService.deleteNotification(id, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete all notifications", description = "Deletes all notifications for the current user.")
    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications() {
        User user = access.currentUser();
        log.trace("deleteAllNotifications called for user {}", user.getUserId());
        notificationService.deleteAllNotifications(user.getUserId());
        return ResponseEntity.noContent().build();
    }
}

