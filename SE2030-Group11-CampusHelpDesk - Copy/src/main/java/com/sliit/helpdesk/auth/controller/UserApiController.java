package com.sliit.helpdesk.auth.controller;

// User Api Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.dto.AdminUserRequest;
import com.sliit.helpdesk.auth.dto.UserResponse;
import com.sliit.helpdesk.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userService.listAll().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public UserResponse detail(@PathVariable Long id) {
        return UserResponse.from(userService.requireById(id));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<UserResponse> create(@Valid @RequestBody AdminUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(userService.create(request)));
    }

    @PutMapping("/{id}")
    @Transactional
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody AdminUserRequest request) {
        return UserResponse.from(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam(name = "permanent", defaultValue = "false") boolean permanent,
            Authentication authentication
    ) {
        userService.deleteAccount(userService.requireByEmail(authentication.getName()), id, permanent);
        return ResponseEntity.noContent().build();
    }
}
