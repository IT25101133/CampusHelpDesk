package com.sliit.helpdesk.auth.controller;

import com.sliit.helpdesk.auth.dto.AuthResponse;
import com.sliit.helpdesk.auth.dto.LoginRequest;
import com.sliit.helpdesk.auth.dto.RegisterRequest;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.security.JwtUtil;
import com.sliit.helpdesk.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthApiController(
            AuthenticationManager authenticationManager,
            AuthService authService,
            JwtUtil jwtUtil
    ) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            return ResponseEntity.ok(toAuthResponse(authentication.getName(), firstRole(authentication)));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        } catch (DisabledException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Account is disabled"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User user = authService.requireByEmail(authentication.getName());
        return ResponseEntity.ok(toAuthResponse(user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toAuthResponse(user.getEmail(), user.getRole().name()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    private AuthResponse toAuthResponse(String email, String role) {
        String fullName = email;
        try {
            fullName = authService.requireByEmail(email).getFullName();
        } catch (IllegalArgumentException ignored) {
            // login still succeeds even if the profile lookup misses
        }
        return new AuthResponse(
                jwtUtil.generateToken(email, role),
                "Bearer",
                email,
                role,
                jwtUtil.getExpirationMs(),
                fullName
        );
    }

    private static String firstRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .orElse("STUDENT");
    }
}
