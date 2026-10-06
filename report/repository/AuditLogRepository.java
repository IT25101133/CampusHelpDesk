package com.sliit.helpdesk.report.repository;

// Audit Log Repository is part of the campus help desk repository code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.report.model.AuditLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @EntityGraph(attributePaths = "user")
    List<AuditLog> findTop20ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "user")
    List<AuditLog> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "user")
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, Long entityId);

    @EntityGraph(attributePaths = "user")
    List<AuditLog> findByUserOrderByCreatedAtDesc(User user);

    @Modifying(clearAutomatically = true)
    @Query("update AuditLog a set a.user = null where a.user.id = :userId")
    int detachUser(@Param("userId") Long userId);
}
