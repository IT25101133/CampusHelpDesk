package com.sliit.helpdesk.ticket.dto;

import jakarta.validation.constraints.NotNull;

public class AssignRequest {

    @NotNull
    private Long assigneeId;

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }
}
