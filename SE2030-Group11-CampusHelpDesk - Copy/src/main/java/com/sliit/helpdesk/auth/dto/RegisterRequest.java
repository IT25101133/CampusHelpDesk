package com.sliit.helpdesk.auth.dto;

// Register Request is part of the campus help desk dto code.

import com.sliit.helpdesk.auth.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    public static final String UNIVERSITY_EMAIL_REGEX =
            "(?i)^[A-Za-z0-9._%+\\-]+@(sliit\\.lk|my\\.sliit\\.lk)$";
    public static final String UNIVERSITY_EMAIL_MESSAGE =
            "Email must be a valid university address (@sliit.lk or @my.sliit.lk)";

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @NotBlank
    @Email
    @Pattern(regexp = UNIVERSITY_EMAIL_REGEX, message = UNIVERSITY_EMAIL_MESSAGE)
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @Size(max = 100, message = "Department must be at most 100 characters")
    private String department;

    @Size(max = 20, message = "Student ID must be at most 20 characters")
    private String studentId;

    private Role role = Role.STUDENT;

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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
