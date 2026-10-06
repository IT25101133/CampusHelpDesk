package com.sliit.helpdesk.auth.service;

/**
 * Stops the chain when the account has been disabled.
 */
public class EnabledAccountHandler extends LoginHandler {

    @Override
    protected void check(LoginAttempt attempt) {
        if (attempt.user() == null || !attempt.user().isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }
    }
}
