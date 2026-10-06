package com.sliit.helpdesk.auth.service;

// Auth Service is part of the campus help desk service code.

import com.sliit.helpdesk.auth.dto.ProfileUpdateRequest;
import com.sliit.helpdesk.auth.dto.RegisterRequest;
import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private static final List<Role> STAFF_ROLES = List.of(
            Role.STAFF, Role.ADMIN, Role.LECTURER, Role.DEPT_HEAD
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("An account with that email already exists.");
        }
        if (request.getRole() != null && request.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Only students can create an account from the sign-in page.");
        }
        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setRole(Role.STUDENT);
        user.setDepartment(blankToNull(request.getDepartment()));
        user.setStudentId(blankToNull(request.getStudentId()));
        return userRepository.save(user);
    }

    public User requireByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<User> staffMembers() {
        return userRepository.findByRoleInOrderByFullNameAsc(STAFF_ROLES);
    }

    @Transactional
    public User updateProfile(User user, ProfileUpdateRequest request) {
        user.setFullName(request.getFullName().trim());
        user.setDepartment(blankToNull(request.getDepartment()));
        user.setStudentId(blankToNull(request.getStudentId()));
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getNewPassword().length() < 8) {
                throw new IllegalArgumentException("New password must be at least 8 characters.");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        return userRepository.save(user);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
