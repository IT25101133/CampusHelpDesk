package com.sliit.helpdesk.auth.service;

import com.sliit.helpdesk.auth.model.User;

/**
 * One sign-in passed along the login handler chain.
 */
public record LoginAttempt(User user, String password) {
}
