package com.sliit.helpdesk.notification.controller;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.notification.model.Notification;
import com.sliit.helpdesk.notification.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @GetMapping("/notifications")
    public String list(Authentication authentication, Model model) {
        User user = authService.requireByEmail(authentication.getName());
        model.addAttribute("notifications", notificationService.inbox(user));
        return "notification/list";
    }

    @PostMapping("/notifications/read-all")
    public String markAll(Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = authService.requireByEmail(authentication.getName());
        notificationService.markAllRead(user);
        redirectAttributes.addFlashAttribute("success", "All notifications marked as read.");
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markOne(@PathVariable Long id, Authentication authentication) {
        User user = authService.requireByEmail(authentication.getName());
        Notification notification = notificationService.markRead(id, user);
        if (notification.getTicket() != null) {
            return "redirect:/tickets/" + notification.getTicket().getId();
        }
        return "redirect:/notifications";
    }
}
