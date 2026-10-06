package com.sliit.helpdesk.report.controller;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.report.dto.AuditLogResponse;
import com.sliit.helpdesk.report.dto.DashboardStats;
import com.sliit.helpdesk.report.dto.ReportResponse;
import com.sliit.helpdesk.report.service.AuditService;
import com.sliit.helpdesk.report.service.ReportService;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    private final ReportService reportService;
    private final AuditService auditService;
    private final AuthService authService;

    public ReportApiController(ReportService reportService, AuditService auditService, AuthService authService) {
        this.reportService = reportService;
        this.auditService = auditService;
        this.authService = authService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ReportResponse reports(Authentication authentication) {
        User user = authService.requireByEmail(authentication.getName());
        ReportResponse response = new ReportResponse();
        response.setStats(reportService.build(user));
        response.setAuditLogs(auditService.recent().stream().map(AuditLogResponse::from).toList());
        return response;
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public DashboardStats dashboard(Authentication authentication) {
        User user = authService.requireByEmail(authentication.getName());
        return reportService.build(user);
    }
}
