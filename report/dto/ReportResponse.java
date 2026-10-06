package com.sliit.helpdesk.report.dto;

// Report Response is part of the campus help desk dto code.

import java.util.ArrayList;
import java.util.List;

public class ReportResponse {

    private DashboardStats stats;
    private List<AuditLogResponse> auditLogs = new ArrayList<>();

    public DashboardStats getStats() {
        return stats;
    }

    public void setStats(DashboardStats stats) {
        this.stats = stats;
    }

    public List<AuditLogResponse> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<AuditLogResponse> auditLogs) {
        this.auditLogs = auditLogs;
    }
}
