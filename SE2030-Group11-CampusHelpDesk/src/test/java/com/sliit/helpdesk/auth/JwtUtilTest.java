package com.sliit.helpdesk.auth;

import com.sliit.helpdesk.auth.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("CampusHelpDeskSecretKeyForHS256TokensMustBe32B!", 3_600_000L);
    }

    @Test
    void generateAndValidateTokenForRole() {
        String token = jwtUtil.generateToken("admin@sliit.lk", "ADMIN");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("admin@sliit.lk");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void stripsRolePrefixWhenGenerating() {
        String token = jwtUtil.generateToken("head@sliit.lk", "ROLE_DEPT_HEAD");

        assertThat(jwtUtil.extractRole(token)).isEqualTo("DEPT_HEAD");
    }

    @Test
    void validateTokenAgainstUserDetails() {
        User user = new User(
                "student@sliit.lk",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.validateToken(token, user)).isTrue();
        assertThat(jwtUtil.extractRole(token)).isEqualTo("STUDENT");
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtUtil.generateToken("staff@sliit.lk", "STAFF");

        assertThat(jwtUtil.validateToken(token + "tampered")).isFalse();
    }
}
