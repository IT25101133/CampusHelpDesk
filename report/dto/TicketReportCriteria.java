package com.sliit.helpdesk.report.dto;

import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional filters shared by the on-screen report and the Excel download.
 * An empty field means that filter is not applied.
 */
public class TicketReportCriteria {

    private Long categoryId;
    private String categoryLabel = "All categories";
    private String department;
    private List<TicketStatus> statuses = new ArrayList<>();
    private List<TicketPriority> priorities = new ArrayList<>();
    private LocalDateTime from;
    private LocalDateTime to;
    private Long assignedToId;
    private String assignedToLabel = "Anyone";
    private Long createdById;
    private String createdByLabel = "Anyone";

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public List<TicketStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<TicketStatus> statuses) {
        this.statuses = statuses == null ? new ArrayList<>() : statuses;
    }

    public List<TicketPriority> getPriorities() {
        return priorities;
    }

    public void setPriorities(List<TicketPriority> priorities) {
        this.priorities = priorities == null ? new ArrayList<>() : priorities;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }

    public Long getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(Long assignedToId) {
        this.assignedToId = assignedToId;
    }

    public String getAssignedToLabel() {
        return assignedToLabel;
    }

    public void setAssignedToLabel(String assignedToLabel) {
        this.assignedToLabel = assignedToLabel;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByLabel() {
        return createdByLabel;
    }

    public void setCreatedByLabel(String createdByLabel) {
        this.createdByLabel = createdByLabel;
    }
}
