package com.sliit.helpdesk.auth.dto;

// User Response is part of the campus help desk dto code.

import com.sliit.helpdesk.auth.model.User;

import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String studentId;
    private String department;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.fullName = user.getFullName();
        response.email = user.getEmail();
        response.role = user.getRole() == null ? null : user.getRole().name();
        response.studentId = user.getStudentId();
        response.department = user.getDepartment();
        response.enabled = user.isEnabled();
        response.createdAt = user.getCreatedAt();
        response.updatedAt = user.getUpdatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getDepartment() {
        return department;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
