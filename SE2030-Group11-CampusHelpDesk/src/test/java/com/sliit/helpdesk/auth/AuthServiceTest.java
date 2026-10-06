package com.sliit.helpdesk.auth;

// Auth Service Test is part of the campus help desk auth code.

import com.sliit.helpdesk.auth.dto.RegisterRequest;
import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void registerCreatesStudentWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName(" Kodagoda Silva ");
        request.setEmail("student@sliit.lk");
        request.setPassword("password1");
        when(userRepository.existsByEmailIgnoreCase("student@sliit.lk")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(saved.getFullName()).isEqualTo("Kodagoda Silva");
        assertThat(saved.getEmail()).isEqualTo("student@sliit.lk");
    }

    @Test
    void registerRejectsNonStudentRole() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Perera Gunawardena");
        request.setEmail("new.lecturer@sliit.lk");
        request.setPassword("password1");
        request.setRole(Role.LECTURER);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only students");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@sliit.lk");
        request.setFullName("Admin");
        request.setPassword("password1");
        when(userRepository.existsByEmailIgnoreCase("admin@sliit.lk")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }
}
