package com.sliit.helpdesk.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Locale;

/**
 * Adds notification columns on SQL Server when Hibernate does not change an existing NOT NULL column.
 * H2 tests already build the table from schema.sql, so this runner leaves them alone.
 */
@Component
public class NotificationColumnPatch implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public NotificationColumnPatch(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (jdbcTemplate.getDataSource() == null) {
            return;
        }
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase(Locale.ROOT).contains("microsoft")) {
                return;
            }
        }
        jdbcTemplate.execute("""
                IF COL_LENGTH('notifications', 'recipient_role') IS NULL
                    ALTER TABLE notifications ADD recipient_role VARCHAR(20) NULL
                """);
        jdbcTemplate.execute("""
                IF COL_LENGTH('notifications', 'link') IS NULL
                    ALTER TABLE notifications ADD link VARCHAR(255) NULL
                """);
        jdbcTemplate.execute("""
                IF EXISTS (
                    SELECT 1 FROM sys.columns
                    WHERE object_id = OBJECT_ID('notifications')
                      AND name = 'user_id' AND is_nullable = 0
                )
                    ALTER TABLE notifications ALTER COLUMN user_id BIGINT NULL
                """);
    }
}
