package com.sliit.helpdesk.report.dto;

// Report Summary Response is part of the campus help desk dto code.

public class ReportSummaryResponse {

    private long totalTickets;
    private long openTickets;
    private long resolvedTickets;
    private long pendingTickets;

    public long getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(long totalTickets) {
        this.totalTickets = totalTickets;
    }

    public long getOpenTickets() {
        return openTickets;
    }

    public void setOpenTickets(long openTickets) {
        this.openTickets = openTickets;
    }

    public long getResolvedTickets() {
        return resolvedTickets;
    }

    public void setResolvedTickets(long resolvedTickets) {
        this.resolvedTickets = resolvedTickets;
    }

    public long getPendingTickets() {
        return pendingTickets;
    }

    public void setPendingTickets(long pendingTickets) {
        this.pendingTickets = pendingTickets;
    }
}
