package com.sliit.helpdesk.auth.service;

/**
 * Thrown when a deactivate, disable, or delete is aimed at an admin account.
 * Admin accounts stay active. There is no extra super-admin role.
 */
public class AdminAccountProtectedException extends RuntimeException {

    public AdminAccountProtectedException() {
        super("Admin accounts cannot be deactivated");
    }
}
