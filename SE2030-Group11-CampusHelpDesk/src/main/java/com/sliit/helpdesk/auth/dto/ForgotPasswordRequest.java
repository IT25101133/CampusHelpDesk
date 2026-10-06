package com.sliit.helpdesk.auth.dto;

// Forgot Password Request is part of the campus help desk dto code.

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 150, message = "Email must be at most 150 characters")
    @Pattern(regexp = RegisterRequest.UNIVERSITY_EMAIL_REGEX, message = RegisterRequest.UNIVERSITY_EMAIL_MESSAGE)
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
