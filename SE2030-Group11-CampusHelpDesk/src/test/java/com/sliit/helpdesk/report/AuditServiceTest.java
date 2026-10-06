package com.sliit.helpdesk.report;

// Audit Service Test is part of the campus help desk report code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.report.model.AuditLog;
import com.sliit.helpdesk.report.repository.AuditLogRepository;
import com.sliit.helpdesk.report.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository, userRepository);
    }

    @Test
    void logWritesUserIdActionAndEntityToAuditLogs() {
        User actor = new User();
        actor.setId(7L);
        actor.setFullName("Nirasha Perera");
        when(userRepository.findById(7L)).thenReturn(Optional.of(actor));

        auditService.log(7L, "Created ticket", "TICKET", 42L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(actor);
        assertThat(saved.getAction()).isEqualTo("Created ticket");
        assertThat(saved.getEntityType()).isEqualTo("TICKET");
        assertThat(saved.getEntityId()).isEqualTo(42L);
    }

    @Test
    void updateChangesActionOnExistingLog() {
        AuditLog existing = new AuditLog();
        existing.setId(3L);
        existing.setAction("Created ticket");
        when(auditLogRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog updated = auditService.update(3L, "Corrected ticket create", "TICKET", 42L);

        assertThat(updated.getAction()).isEqualTo("Corrected ticket create");
        assertThat(updated.getEntityType()).isEqualTo("TICKET");
        assertThat(updated.getEntityId()).isEqualTo(42L);
    }
}
