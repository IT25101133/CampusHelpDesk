package com.sliit.helpdesk.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Stops the chain when the password does not match the stored hash.
 */
public class PasswordMatchHandler extends LoginHandler {

    private final PasswordEncoder passwordEncoder;

    public PasswordMatchHandler(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void check(LoginAttempt attempt) {
        String password = attempt.password();
        if (password == null || !passwordEncoder.matches(password, attempt.user().getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
    }
}
