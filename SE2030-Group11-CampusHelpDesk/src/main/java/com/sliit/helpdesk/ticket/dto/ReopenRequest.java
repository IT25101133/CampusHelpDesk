package com.sliit.helpdesk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Sent when someone reopens a closed ticket. */
public class ReopenRequest {

    /** Why the ticket should be opened again. This is saved in the history. */
    @NotBlank(message = "A reason is required to reopen this ticket.")
    @Size(max = 500, message = "Reason must be at most 500 characters.")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
