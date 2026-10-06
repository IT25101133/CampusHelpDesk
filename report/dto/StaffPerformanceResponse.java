package com.sliit.helpdesk.report.dto;

// Staff Performance Response is part of the campus help desk dto code.

public class StaffPerformanceResponse {

    private Long staffId;
    private String staffName;
    private long ticketsResolved;

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public long getTicketsResolved() {
        return ticketsResolved;
    }

    public void setTicketsResolved(long ticketsResolved) {
        this.ticketsResolved = ticketsResolved;
    }
}
