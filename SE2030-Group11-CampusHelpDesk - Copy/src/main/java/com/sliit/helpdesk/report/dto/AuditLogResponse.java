package com.sliit.helpdesk.report.dto;

import com.sliit.helpdesk.report.model.AuditLog;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String actor;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.id = log.getId();
        response.action = log.getAction();
        response.entityType = log.getEntityType();
        response.entityId = log.getEntityId();
        response.actor = log.getUser() == null ? null : log.getUser().getFullName();
        response.createdAt = log.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getActor() {
        return actor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
