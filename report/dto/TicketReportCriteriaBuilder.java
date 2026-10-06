package com.sliit.helpdesk.report.dto;

import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Builder for one filtered report.
 * Each call sets one part of the filter, and {@link #build()} returns the finished criteria.
 */
public class TicketReportCriteriaBuilder {

    private final TicketReportCriteria criteria = new TicketReportCriteria();

    public TicketReportCriteriaBuilder category(Long categoryId, String categoryLabel) {
        criteria.setCategoryId(categoryId);
        if (categoryLabel != null && !categoryLabel.isBlank()) {
            criteria.setCategoryLabel(categoryLabel);
        }
        return this;
    }

    public TicketReportCriteriaBuilder department(String department) {
        criteria.setDepartment(department);
        return this;
    }

    public TicketReportCriteriaBuilder statuses(List<TicketStatus> statuses) {
        criteria.setStatuses(statuses);
        return this;
    }

    public TicketReportCriteriaBuilder priorities(List<TicketPriority> priorities) {
        criteria.setPriorities(priorities);
        return this;
    }

    public TicketReportCriteriaBuilder from(LocalDateTime from) {
        criteria.setFrom(from);
        return this;
    }

    public TicketReportCriteriaBuilder to(LocalDateTime to) {
        criteria.setTo(to);
        return this;
    }

    public TicketReportCriteriaBuilder assignedTo(Long assignedToId, String assignedToLabel) {
        criteria.setAssignedToId(assignedToId);
        if (assignedToLabel != null && !assignedToLabel.isBlank()) {
            criteria.setAssignedToLabel(assignedToLabel);
        }
        return this;
    }

    public TicketReportCriteriaBuilder createdBy(Long createdById, String createdByLabel) {
        criteria.setCreatedById(createdById);
        if (createdByLabel != null && !createdByLabel.isBlank()) {
            criteria.setCreatedByLabel(createdByLabel);
        }
        return this;
    }

    public TicketReportCriteria build() {
        return criteria;
    }
}
