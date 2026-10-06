package com.sliit.helpdesk.ticket.dto;

// Comment Request is part of the campus help desk dto code.

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Sent when someone adds a reply on a ticket. */
public class CommentRequest {

    /** The reply text. */
    @NotBlank(message = "Comment cannot be empty.")
    @Size(max = 4000, message = "Comment must be at most 4000 characters.")
    private String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
