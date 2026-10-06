package com.sliit.helpdesk.config;

// Demo User Seeder is part of the campus help desk config code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class DemoUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);
    private static final String DEMO_PASSWORD = "password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;
        created += ensure("Nirasha Perera", "admin@sliit.lk", Role.ADMIN, null, "IT Services");
        created += ensure("Jayathilaka Fernando", "head@sliit.lk", Role.DEPT_HEAD, null, "Faculty of Computing");
        created += ensure("Perera Gunawardena", "lecturer@sliit.lk", Role.LECTURER, null, "Faculty of Computing");
        created += ensure("Umer Farook", "staff@sliit.lk", Role.STAFF, null, "Facilities");
        created += ensure("Arachchi Dissanayake", "it.support@sliit.lk", Role.STAFF, null, "IT Services");
        created += ensure("Kodagoda Silva", "student@sliit.lk", Role.STUDENT, "IT20201234", "Faculty of Computing");
        if (created > 0) {
            log.info("Seeded {} demo logins (password: {})", created, DEMO_PASSWORD);
        }
    }

    private int ensure(String fullName, String email, Role role, String studentId, String department) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return 0;
        }
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setStudentId(studentId);
        user.setDepartment(department);
        user.setEnabled(true);
        userRepository.save(user);
        return 1;
    }
}
