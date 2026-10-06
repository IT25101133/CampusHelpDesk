package com.sliit.helpdesk.auth.dto;

// Auth Response is part of the campus help desk dto code.

public class AuthResponse {

    private final String token;
    private final String tokenType;
    private final String email;
    private final String role;
    private final String fullName;
    private final long expiresIn;
    private final Long userId;

    public AuthResponse(String token, String tokenType, String email, String role, long expiresIn) {
        this(token, tokenType, email, role, expiresIn, null, null);
    }

    public AuthResponse(String token, String tokenType, String email, String role, long expiresIn, String fullName) {
        this(token, tokenType, email, role, expiresIn, fullName, null);
    }

    public AuthResponse(String token, String tokenType, String email, String role, long expiresIn, String fullName, Long userId) {
        this.token = token;
        this.tokenType = tokenType;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
        this.fullName = fullName;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getFullName() {
        return fullName;
    }

    public Long getUserId() {
        return userId;
    }
}
