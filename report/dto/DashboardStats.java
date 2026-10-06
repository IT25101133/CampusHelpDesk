package com.sliit.helpdesk.report.dto;

// Dashboard Stats is part of the campus help desk dto code.

import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardStats {

    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long closedTickets;
    private long unreadNotifications;
    private Map<String, Long> byStatus = new LinkedHashMap<>();
    private Map<String, Long> byPriority = new LinkedHashMap<>();
    private Map<String, Long> byCategory = new LinkedHashMap<>();

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

    public long getInProgressTickets() {
        return inProgressTickets;
    }

    public void setInProgressTickets(long inProgressTickets) {
        this.inProgressTickets = inProgressTickets;
    }

    public long getResolvedTickets() {
        return resolvedTickets;
    }

    public void setResolvedTickets(long resolvedTickets) {
        this.resolvedTickets = resolvedTickets;
    }

    public long getClosedTickets() {
        return closedTickets;
    }

    public void setClosedTickets(long closedTickets) {
        this.closedTickets = closedTickets;
    }

    public long getUnreadNotifications() {
        return unreadNotifications;
    }

    public void setUnreadNotifications(long unreadNotifications) {
        this.unreadNotifications = unreadNotifications;
    }

    public Map<String, Long> getByStatus() {
        return byStatus;
    }

    public void setByStatus(Map<String, Long> byStatus) {
        this.byStatus = byStatus;
    }

    public Map<String, Long> getByPriority() {
        return byPriority;
    }

    public void setByPriority(Map<String, Long> byPriority) {
        this.byPriority = byPriority;
    }

    public Map<String, Long> getByCategory() {
        return byCategory;
    }

    public void setByCategory(Map<String, Long> byCategory) {
        this.byCategory = byCategory;
    }
}
