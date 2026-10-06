package com.sliit.helpdesk.auth.controller;

// Auth Api Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.dto.AuthResponse;
import com.sliit.helpdesk.auth.dto.ForgotPasswordRequest;
import com.sliit.helpdesk.auth.dto.LoginRequest;
import com.sliit.helpdesk.auth.dto.ProfileUpdateRequest;
import com.sliit.helpdesk.auth.dto.RegisterRequest;
import com.sliit.helpdesk.auth.dto.ResetPasswordRequest;
import com.sliit.helpdesk.auth.dto.UserResponse;
import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.security.JwtUtil;
import com.sliit.helpdesk.auth.service.PasswordResetService;
import com.sliit.helpdesk.auth.service.UserService;
import com.sliit.helpdesk.report.service.AuditEventService;
import com.sliit.helpdesk.report.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;
    private final AuditService auditService;
    private final AuditEventService auditEventService;

    public AuthApiController(
            UserService userService,
            JwtUtil jwtUtil,
            PasswordResetService passwordResetService,
            AuditService auditService,
            AuditEventService auditEventService
    ) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordResetService = passwordResetService;
        this.auditService = auditService;
        this.auditEventService = auditEventService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User saved = userService.register(toUser(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toAuthResponse(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.getEmail(), request.getPassword());
        User user = userService.requireByEmail(request.getEmail());
        return ResponseEntity.ok(new AuthResponse(
                token,
                "Bearer",
                user.getEmail(),
                user.getRole().name(),
                jwtUtil.getExpirationMs(),
                user.getFullName(),
                user.getId()
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String token = passwordResetService.issueToken(request.getEmail());
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", "If that university email is registered, a reset token was generated.");
        if (token != null) {
            body.put("resetToken", token);
            body.put("resetPath", "/reset?token=" + token);
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated. You can sign in with the new password."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(toAuthResponse(user));
    }

    @GetMapping("/profile")
    public UserResponse profile(Authentication authentication) {
        return UserResponse.from(currentUser(authentication));
    }

    @GetMapping("/activity")
    @PreAuthorize("#userId == null or hasRole('ADMIN') or @accountGuards.isSelf(authentication, #userId)")
    public Map<String, Object> activity(
            Authentication authentication,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        User actor = currentUser(authentication);
        User subject = actor;
        if (userId != null && !userId.equals(actor.getId())) {
            if (actor.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("Forbidden");
            }
            subject = userService.requireById(userId);
        }
        return auditEventService.page(subject, action, from, to, page);
    }

    @PutMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        User updated = userService.updateProfile(currentUser(authentication), request);
        return ResponseEntity.ok(toAuthResponse(updated));
    }

    @PutMapping("/deactivate/{userId}")
    @PreAuthorize("@accountGuards.mayReachDeactivate(authentication, #userId)")
    public ResponseEntity<Map<String, String>> deactivateAccount(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        User current = currentUser(authentication);
        userService.deactivateAccount(current, userId);
        return ResponseEntity.ok(Map.of("message", "Account deactivated"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        currentUser(authentication);
        return ResponseEntity.ok(Map.of("message", "Signed out"));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return userService.requireByEmail(authentication.getName());
    }

    private static User toUser(RegisterRequest request) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setDepartment(request.getDepartment());
        user.setStudentId(request.getStudentId());
        if (request.getRole() != null && request.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Only students can create an account from the sign-in page.");
        }
        user.setRole(Role.STUDENT);
        return user;
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                jwtUtil.generateToken(user.getEmail(), user.getRole().name()),
                "Bearer",
                user.getEmail(),
                user.getRole().name(),
                jwtUtil.getExpirationMs(),
                user.getFullName(),
                user.getId()
        );
    }
}
