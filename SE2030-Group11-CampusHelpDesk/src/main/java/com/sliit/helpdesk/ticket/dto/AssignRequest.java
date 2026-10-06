package com.sliit.helpdesk.ticket.dto;

// Assign Request is part of the campus help desk dto code.

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Sent when staff assign a ticket to someone. */
public class AssignRequest {

    /** Id of the staff member who should work on the ticket. */
    @NotNull(message = "Assignee is required")
    @Positive(message = "Assignee is required")
    private Long assigneeId;

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }
}
