package com.sliit.helpdesk.auth;

// User Service Test is part of the campus help desk auth code.

import com.sliit.helpdesk.auth.dto.AdminUserRequest;
import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.auth.security.JwtUtil;
import com.sliit.helpdesk.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void registerSucceedsForNewUniversityEmail() {
        User incoming = newUser("student@sliit.lk", "password1", " Kodagoda Silva ");
        when(userRepository.findByEmail("student@sliit.lk")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("student@sliit.lk")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.register(incoming);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(captor.getValue().isEnabled()).isTrue();
        assertThat(saved.getEmail()).isEqualTo("student@sliit.lk");
        assertThat(saved.getFullName()).isEqualTo("Kodagoda Silva");
    }

    @Test
    void registerKeepsAssignedCampusRole() {
        User incoming = newUser("head.new@sliit.lk", "password1", "Jayathilaka Fernando");
        incoming.setRole(Role.DEPT_HEAD);
        when(userRepository.findByEmail("head.new@sliit.lk")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("head.new@sliit.lk")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.register(incoming);

        assertThat(saved.getRole()).isEqualTo(Role.DEPT_HEAD);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        User incoming = newUser("admin@sliit.lk", "password1", "Admin");
        when(userRepository.findByEmail("admin@sliit.lk")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.register(incoming))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerHashesPasswordWithBcrypt() {
        User incoming = newUser("student@sliit.lk", "password1", "Kodagoda Silva");
        when(userRepository.findByEmail("student@sliit.lk")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.register(incoming);

        assertThat(saved.getPassword()).isNotEqualTo("password1");
        assertThat(saved.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("password1", saved.getPassword())).isTrue();
    }

    @Test
    void loginFailsOnWrongPassword() {
        User stored = newUser("student@sliit.lk", passwordEncoder.encode("password1"), "Kodagoda Silva");
        stored.setRole(Role.STUDENT);
        stored.setEnabled(true);
        when(userRepository.findByEmail("student@sliit.lk")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> userService.login("student@sliit.lk", "wrong-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email or password");
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    void createAdminUserHashesPasswordAndSetsRole() {
        AdminUserRequest request = new AdminUserRequest();
        request.setFullName("Viva Staff");
        request.setEmail("viva.staff@sliit.lk");
        request.setPassword("password1");
        request.setRole(Role.STAFF);
        when(userRepository.findByEmail("viva.staff@sliit.lk")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.create(request);

        assertThat(saved.getRole()).isEqualTo(Role.STAFF);
        assertThat(saved.getPassword()).startsWith("$2a$");
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void deleteDeactivatesAccount() {
        User stored = newUser("student@sliit.lk", passwordEncoder.encode("password1"), "Kodagoda Silva");
        stored.setId(9L);
        stored.setEnabled(true);
        when(userRepository.findById(9L)).thenReturn(Optional.of(stored));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.delete(9L);

        assertThat(stored.isEnabled()).isFalse();
    }

    private static User newUser(String email, String password, String fullName) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setFullName(fullName);
        return user;
    }
}
