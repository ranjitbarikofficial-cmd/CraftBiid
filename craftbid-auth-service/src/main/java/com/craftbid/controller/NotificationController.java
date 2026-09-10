package com.craftbid.controller;

import com.craftbid.entity.Notification;
import com.craftbid.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(notificationService.getMyNotifications(identifier));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        String identifier = authentication.getName();
        long count = notificationService.getUnreadCount(identifier);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(Authentication authentication, @PathVariable Long id) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(notificationService.markAsRead(identifier, id));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllAsRead(Authentication authentication) {
        String identifier = authentication.getName();
        notificationService.markAllAsRead(identifier);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
