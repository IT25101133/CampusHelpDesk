package com.sliit.helpdesk.ticket.dto;

// Ticket Feedback Request is part of the campus help desk dto code.

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Sent by the ticket owner after the ticket is closed. */
public class TicketFeedbackRequest {

    /** Score from 1 (poor) to 5 (excellent). */
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int rating;

    /** Optional note about how the ticket was handled. */
    @Size(max = 1000, message = "Comment must be at most 1000 characters")
    private String comment;

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
