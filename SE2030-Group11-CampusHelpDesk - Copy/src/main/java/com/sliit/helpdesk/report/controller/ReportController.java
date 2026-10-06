package com.sliit.helpdesk.report.controller;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.report.service.AuditService;
import com.sliit.helpdesk.report.service.ReportService;
import com.sliit.helpdesk.ticket.service.TicketService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportController {

    private final ReportService reportService;
    private final TicketService ticketService;
    private final AuthService authService;
    private final AuditService auditService;

    public ReportController(
            ReportService reportService,
            TicketService ticketService,
            AuthService authService,
            AuditService auditService
    ) {
        this.reportService = reportService;
        this.ticketService = ticketService;
        this.authService = authService;
        this.auditService = auditService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = authService.requireByEmail(authentication.getName());
        model.addAttribute("stats", reportService.build(user));
        model.addAttribute("recentTickets", ticketService.listFor(user).stream().limit(6).toList());
        return "report/dashboard";
    }

    @GetMapping("/reports")
    public String reports(Authentication authentication, Model model) {
        User user = authService.requireByEmail(authentication.getName());
        model.addAttribute("stats", reportService.build(user));
        model.addAttribute("auditLogs", auditService.recent());
        return "report/reports";
    }
}
