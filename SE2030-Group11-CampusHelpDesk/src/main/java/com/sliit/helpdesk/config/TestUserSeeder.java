package com.sliit.helpdesk.config;

// Test User Seeder is part of the campus help desk config code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestUserSeeder implements ApplicationRunner {

    public static final String TEST_EMAIL = "test@sliit.lk";
    public static final String TEST_PASSWORD = "Test1234";

    private static final Logger log = LoggerFactory.getLogger(TestUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TestUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(TEST_EMAIL)) {
            return;
        }

        User user = new User();
        user.setFullName("Test Student");
        user.setEmail(TEST_EMAIL);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(Role.STUDENT);
        user.setStudentId("IT99999999");
        user.setDepartment("Faculty of Computing");
        user.setEnabled(true);
        userRepository.save(user);
        log.info("Created test login {} / {}", TEST_EMAIL, TEST_PASSWORD);
    }
}
