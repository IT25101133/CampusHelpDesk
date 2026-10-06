package com.sliit.helpdesk.auth.dto;

// Reset Password Request is part of the campus help desk dto code.

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Size(max = 255, message = "Reset token is too long")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
    private String newPassword;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
