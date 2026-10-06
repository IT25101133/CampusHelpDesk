package com.sliit.helpdesk.ticket.dto;

// Ticket Form is part of the campus help desk dto code.

import com.sliit.helpdesk.ticket.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** CREATE body. The form sent when someone submits a new ticket. */
public class TicketForm { 

    /** Short name of the problem. */
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    /** What went wrong, in the user's own words. */
    @NotBlank(message = "Description is required")
    @Size(max = 4000, message = "Description must be at most 4000 characters")
    private String description;

    /** Which department should handle this ticket. */
    @NotNull(message = "Please select a department")
    @Positive(message = "Please select a department")
    private Long categoryId;

    /** How urgent the ticket is. Defaults to medium. */
    @NotNull(message = "Priority is required")
    private TicketPriority priority = TicketPriority.MEDIUM;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }
}
