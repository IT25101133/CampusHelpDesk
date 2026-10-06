package com.sliit.helpdesk.auth.service;

/**
 * Chain of Responsibility for sign-in checks.
 * Each handler does one check, then passes the attempt to the next handler.
 */
public abstract class LoginHandler {

    private LoginHandler next;

    public LoginHandler link(LoginHandler next) {
        this.next = next;
        return next;
    }

    public final void handle(LoginAttempt attempt) {
        check(attempt);
        if (next != null) {
            next.handle(attempt);
        }
    }

    protected abstract void check(LoginAttempt attempt);
}
