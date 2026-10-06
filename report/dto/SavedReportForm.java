package com.sliit.helpdesk.report.dto;

import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Fields posted from the saved-report edit form.
 */
public class SavedReportForm {

    @NotBlank(message = "Report name is required")
    @Size(max = 150, message = "Report name must be at most 150 characters")
    private String name;

    @Size(max = 100, message = "Department must be at most 100 characters")
    private String department;
    private TicketStatus status;
    private TicketPriority priority;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }
}
