package com.sliit.helpdesk.report.service;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.report.model.AuditLog;
import com.sliit.helpdesk.report.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(User user, String action, String entityType, Long entityId) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        auditLogRepository.save(log);
    }

    public List<AuditLog> recent() {
        return auditLogRepository.findTop20ByOrderByCreatedAtDesc();
    }
}
