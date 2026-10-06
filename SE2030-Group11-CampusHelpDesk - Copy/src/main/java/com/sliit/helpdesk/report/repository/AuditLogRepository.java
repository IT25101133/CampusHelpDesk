package com.sliit.helpdesk.report.repository;

import com.sliit.helpdesk.report.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop20ByOrderByCreatedAtDesc();
}
