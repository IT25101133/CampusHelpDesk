package com.sliit.helpdesk.auth.service;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * SpEL helpers so Spring Security can reject the wrong role before the service method runs.
 */
@Component("accountGuards")
public class AccountGuards {

    private final UserRepository userRepository;

    public AccountGuards(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isSelf(Authentication authentication, Long userId) {
        User actor = actor(authentication);
        return actor != null && userId != null && userId.equals(actor.getId());
    }

    /**
     * Students and staff may only open deactivate for their own id.
     * Admins may open it for any id; the service still refuses an admin target.
     */
    public boolean mayReachDeactivate(Authentication authentication, Long userId) {
        User actor = actor(authentication);
        if (actor == null || userId == null) {
            return false;
        }
        if (actor.getRole() == Role.ADMIN) {
            return true;
        }
        return userId.equals(actor.getId());
    }

    public boolean isSelfOrAdmin(Authentication authentication, Long userId) {
        User actor = actor(authentication);
        if (actor == null || userId == null) {
            return false;
        }
        return actor.getRole() == Role.ADMIN || userId.equals(actor.getId());
    }

    private User actor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
    }
}
