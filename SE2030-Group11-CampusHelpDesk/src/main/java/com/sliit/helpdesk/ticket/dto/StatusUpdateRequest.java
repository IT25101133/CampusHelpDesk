package com.sliit.helpdesk.ticket.dto;

// Status Update Request is part of the campus help desk dto code.

import com.sliit.helpdesk.ticket.model.TicketStatus;
import jakarta.validation.constraints.NotNull;

/** Sent when the ticket status is changed. */
public class StatusUpdateRequest {

    /** New status, such as Open, In Progress, Resolved, or Closed. */
    @NotNull(message = "Status is required")
    private TicketStatus status;

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }
}
