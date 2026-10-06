package com.sliit.helpdesk.config;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.notification.repository.NotificationRepository;
import com.sliit.helpdesk.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Inserts one welcome row for each active account so every role sees the bell.
 */
@Component
@Order(20)
public class WelcomeNotificationSeeder implements ApplicationRunner {

    public static final String WELCOME = "Welcome to Campus Help Desk. Your inbox is ready.";

    private static final Logger log = LoggerFactory.getLogger(WelcomeNotificationSeeder.class);

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public WelcomeNotificationSeeder(
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Role role : Role.values()) {
            for (var user : userRepository.findByEnabledTrueAndRoleInOrderByIdAsc(java.util.List.of(role))) {
                if (notificationRepository.existsByRecipientAndMessageStartingWith(user, "Welcome to Campus Help Desk")) {
                    continue;
                }
                notificationService.notify(user, WELCOME, null);
                log.info("Welcome notification created for {}", user.getEmail());
            }
        }
    }
}
