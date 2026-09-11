package com.sliit.helpdesk.auth.dto;

public class AuthResponse {

    private final String token;
    private final String tokenType;
    private final String email;
    private final String role;
    private final String fullName;
    private final long expiresIn;

    public AuthResponse(String token, String tokenType, String email, String role, long expiresIn) {
        this(token, tokenType, email, role, expiresIn, null);
    }

    public AuthResponse(String token, String tokenType, String email, String role, long expiresIn, String fullName) {
        this.token = token;
        this.tokenType = tokenType;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
        this.fullName = fullName;
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
}
