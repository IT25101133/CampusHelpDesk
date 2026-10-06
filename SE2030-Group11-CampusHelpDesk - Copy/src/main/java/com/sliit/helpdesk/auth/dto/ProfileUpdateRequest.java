package com.sliit.helpdesk.auth.dto;

// Profile Update Request is part of the campus help desk dto code.

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @Size(max = 100, message = "Department must be at most 100 characters")
    private String department;

    @Size(max = 20, message = "Student ID must be at most 20 characters")
    private String studentId;

    @Size(max = 72, message = "New password must be at most 72 characters")
    @Pattern(regexp = "^$|^.{8,72}$", message = "New password must be at least 8 characters")
    private String newPassword;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
