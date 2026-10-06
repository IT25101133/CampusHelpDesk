package com.sliit.helpdesk.auth.dto;

// Admin User Request is part of the campus help desk dto code.

import com.sliit.helpdesk.auth.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AdminUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @NotBlank
    @Email
    @Pattern(regexp = RegisterRequest.UNIVERSITY_EMAIL_REGEX, message = RegisterRequest.UNIVERSITY_EMAIL_MESSAGE)
    @Size(max = 150)
    private String email;

    @Pattern(regexp = "^$|^.{8,72}$", message = "Password must be between 8 and 72 characters")
    private String password;

    private Role role = Role.STUDENT;

    @Size(max = 100)
    private String department;

    @Size(max = 20)
    private String studentId;

    private Boolean enabled;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
