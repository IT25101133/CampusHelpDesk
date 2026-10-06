package com.sliit.helpdesk.report.dto;

// Audit Log Request is part of the campus help desk dto code.

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuditLogRequest {

    @NotBlank(message = "Action is required")
    @Size(max = 200, message = "Action must be at most 200 characters")
    private String action;

    @Size(max = 50, message = "Entity type must be at most 50 characters")
    private String entityType;

    @jakarta.validation.constraints.Positive(message = "Entity id must be a positive number")
    private Long entityId;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
}
