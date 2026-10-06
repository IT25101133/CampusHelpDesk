package com.sliit.helpdesk.report.dto;

// Overdue Ticket Response is part of the campus help desk dto code.

import com.sliit.helpdesk.ticket.model.TicketStatus;

import java.time.LocalDateTime;

public class OverdueTicketResponse {

    private Long id;
    private String ticketNumber;
    private String title;
    private TicketStatus status;
    private String category;
    private int slaHours;
    private LocalDateTime createdAt;
    private LocalDateTime dueAt;
    private double hoursOverdue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(int slaHours) {
        this.slaHours = slaHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public double getHoursOverdue() {
        return hoursOverdue;
    }

    public void setHoursOverdue(double hoursOverdue) {
        this.hoursOverdue = hoursOverdue;
    }
}
