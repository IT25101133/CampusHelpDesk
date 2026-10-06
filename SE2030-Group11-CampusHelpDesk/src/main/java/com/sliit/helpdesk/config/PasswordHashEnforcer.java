package com.sliit.helpdesk.config;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stores only BCrypt hashes in users.password_hash and makes that column NOT NULL
 * on the live database (SQL Server Management Studio included).
 */
@Component
@Order(5)
public class PasswordHashEnforcer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordHashEnforcer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public PasswordHashEnforcer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        int hashed = 0;
        for (User user : userRepository.findAll()) {
            String stored = user.getPassword();
            if (isBcrypt(stored)) {
                continue;
            }
            String raw = stored == null || stored.isBlank()
                    ? UUID.randomUUID().toString()
                    : stored;
            user.setPassword(passwordEncoder.encode(raw));
            userRepository.save(user);
            hashed++;
        }
        if (hashed > 0) {
            log.info("Replaced {} plaintext or empty password(s) with BCrypt hashes", hashed);
        }
        requirePasswordColumn();
    }

    private void requirePasswordColumn() {
        String nullable = jdbcTemplate.query(
                "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE LOWER(TABLE_NAME) = 'users' AND LOWER(COLUMN_NAME) = 'password_hash'",
                rs -> rs.next() ? rs.getString(1) : null
        );
        if (nullable == null || "NO".equalsIgnoreCase(nullable)) {
            return;
        }
        jdbcTemplate.update(
                "ALTER TABLE users ALTER COLUMN password_hash VARCHAR(255) NOT NULL"
        );
        log.info("Set users.password_hash to NOT NULL");
    }

    static boolean isBcrypt(String stored) {
        return stored != null && stored.startsWith("$2");
    }
}
