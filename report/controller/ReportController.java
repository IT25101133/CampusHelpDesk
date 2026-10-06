package com.sliit.helpdesk.report.controller;

// Report Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.report.service.AuditService;
import com.sliit.helpdesk.report.service.ReportService;
import com.sliit.helpdesk.ticket.service.TicketService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("resolutionStats", reportService.resolutionStats());
        return "report/dashboard";
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Authentication authentication, Model model) {
        return dashboard(authentication, model);
    }

    @GetMapping("/reports")
    public String reports(Authentication authentication, Model model) {
        User user = authService.requireByEmail(authentication.getName());
        model.addAttribute("stats", reportService.build(user));
        model.addAttribute("auditLogs", auditService.recent());
        return "report/reports";
    }

    @PostMapping("/reports/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public String createAuditLog(
            Authentication authentication,
            @RequestParam String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            auditService.create(user, action, entityType, entityId);
            redirectAttributes.addFlashAttribute("success", "Audit log created.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reports";
    }

    @PostMapping("/reports/audit-logs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateAuditLog(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            auditService.update(id, action, entityType, entityId);
            redirectAttributes.addFlashAttribute("success", "Audit log updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reports";
    }

    @PostMapping("/reports/audit-logs/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteAuditLog(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            auditService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Audit log deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reports";
    }
}
