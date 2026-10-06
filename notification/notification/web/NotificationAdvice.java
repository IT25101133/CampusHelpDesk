package com.sliit.helpdesk.notification.web;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.notification.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NotificationAdvice {

    private final AuthService authService;
    private final NotificationService notificationService;

    public NotificationAdvice(AuthService authService, NotificationService notificationService) {
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @ModelAttribute("unreadCount")
    public long unreadCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return 0;
        }
        String name = authentication.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return 0;
        }
        try {
            User user = authService.requireByEmail(name);
            return notificationService.unreadCount(user);
        } catch (IllegalArgumentException ex) {
            return 0;
        }
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return null;
        }
        try {
            return authService.requireByEmail(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
