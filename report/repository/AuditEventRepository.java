package com.sliit.helpdesk.report.repository;

// Audit Event Repository is part of the campus help desk repository code.

import com.sliit.helpdesk.report.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("""
            select e from AuditEvent e
            where e.user.id = :userId
              and (:action is null or e.action = :action)
              and (:from is null or e.createdAt >= :from)
              and (:to is null or e.createdAt <= :to)
            """)
    Page<AuditEvent> search(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("update AuditEvent e set e.user = null where e.user.id = :userId")
    int detachUser(@Param("userId") Long userId);
}
