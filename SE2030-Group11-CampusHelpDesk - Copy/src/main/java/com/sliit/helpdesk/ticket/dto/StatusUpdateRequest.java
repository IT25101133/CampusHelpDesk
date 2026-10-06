package com.sliit.helpdesk.ticket.dto;

import com.sliit.helpdesk.ticket.model.TicketStatus;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateRequest {

    @NotNull
    private TicketStatus status;

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }
}
