package com.sliit.helpdesk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Sent when staff close a resolved ticket. */
public class CloseRequest {

    /** Why the ticket is being closed. Saved on the ticket history. */
    @NotBlank(message = "A reason is required to close this ticket.")
    @Size(max = 500, message = "Reason must be at most 500 characters.")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
