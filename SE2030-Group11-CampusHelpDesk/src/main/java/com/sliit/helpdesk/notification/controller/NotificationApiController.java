package com.sliit.helpdesk.notification.controller;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.notification.dto.NotificationResponse;
import com.sliit.helpdesk.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationApiController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Authentication authentication) {
        User user = currentUser(authentication);
        return notificationService.inbox(user).stream().map(NotificationResponse::from).toList();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAll(Authentication authentication) {
        notificationService.markAllRead(currentUser(authentication));
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markOne(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(NotificationResponse.from(
                notificationService.markRead(id, currentUser(authentication))
        ));
    }

    private User currentUser(Authentication authentication) {
        return authService.requireByEmail(authentication.getName());
    }
}
