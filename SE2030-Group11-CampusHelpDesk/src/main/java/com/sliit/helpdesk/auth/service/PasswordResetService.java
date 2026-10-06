package com.sliit.helpdesk.auth.service;

// Password Reset Service is part of the campus help desk service code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    private static final int TOKEN_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final UserService userService;
    private final Map<String, ResetToken> tokens = new ConcurrentHashMap<>();

    public PasswordResetService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public String issueToken(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalized)
                .or(() -> userRepository.findByEmailIgnoreCase(email.trim()))
                .orElse(null);
        if (user == null || !user.isEnabled()) {
            return null;
        }
        tokens.entrySet().removeIf(entry -> entry.getValue().expired()
                || normalized.equalsIgnoreCase(entry.getValue().email()));
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new ResetToken(normalized, Instant.now().plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES)));
        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Reset token is invalid or expired.");
        }
        ResetToken entry = tokens.remove(token.trim());
        if (entry == null || entry.expired()) {
            throw new IllegalArgumentException("Reset token is invalid or expired.");
        }
        userService.resetPassword(entry.email(), newPassword);
    }

    private record ResetToken(String email, Instant expiresAt) {
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
