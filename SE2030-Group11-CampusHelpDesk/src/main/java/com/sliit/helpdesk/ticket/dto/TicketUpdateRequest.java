package com.sliit.helpdesk.ticket.dto;

// Ticket Update Request is part of the campus help desk dto code.

import com.sliit.helpdesk.ticket.model.TicketPriority;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** UPDATE body. Only the fields that were sent are changed. */
public class TicketUpdateRequest {

    /** New title. Left empty means "do not change the title". */
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    /** New description. Left empty means "do not change the description". */
    @Size(max = 4000, message = "Description must be at most 4000 characters")
    private String description;

    /** New urgency. Staff only. Students and lecturers cannot change this. */
    private TicketPriority priority;

    /** New department or category. */
    @Positive(message = "Please select a valid category")
    private Long categoryId;

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

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
