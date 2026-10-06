package com.sliit.helpdesk.report.service;

// Audit Service is part of the campus help desk service code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.report.model.AuditLog;
import com.sliit.helpdesk.report.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void log(Long userId, String action, String entityType, Long entityId) {
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);
        log(user, action, entityType, entityId);
    }

    @Transactional
    public AuditLog log(User user, String action, String entityType, Long entityId) {
        AuditLog entry = new AuditLog();
        entry.setUser(user);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        return auditLogRepository.save(entry);
    }

    @Transactional
    public void record(User user, String action, String entityType, Long entityId) {
        log(user, action, entityType, entityId);
    }

    public AuditLog require(Long id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Audit log not found"));
    }

    @Transactional
    public AuditLog create(User user, String action, String entityType, Long entityId) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action is required");
        }
        return log(user, action.trim(), blankToNull(entityType), entityId);
    }

    @Transactional
    public AuditLog update(Long id, String action, String entityType, Long entityId) {
        AuditLog entry = require(id);
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action is required");
        }
        entry.setAction(action.trim());
        entry.setEntityType(blankToNull(entityType));
        entry.setEntityId(entityId);
        return auditLogRepository.save(entry);
    }

    @Transactional
    public void delete(Long id) {
        auditLogRepository.delete(require(id));
    }

    public List<AuditLog> recent() {
        return auditLogRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public List<AuditLog> history() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<AuditLog> forEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId);
    }

    public List<AuditLog> forUser(User user) {
        return auditLogRepository.findByUserOrderByCreatedAtDesc(user);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
