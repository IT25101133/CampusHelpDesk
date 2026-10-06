package com.sliit.helpdesk.report.controller;

// Report Api Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.report.dto.AuditLogRequest;
import com.sliit.helpdesk.report.dto.AuditLogResponse;
import com.sliit.helpdesk.report.dto.CategoryCountResponse;
import com.sliit.helpdesk.report.dto.DashboardStats;
import com.sliit.helpdesk.report.dto.OverdueTicketResponse;
import com.sliit.helpdesk.report.dto.ReportResponse;
import com.sliit.helpdesk.report.dto.ReportSummaryResponse;
import com.sliit.helpdesk.report.dto.ResolutionTimeAvgResponse;
import com.sliit.helpdesk.report.dto.StaffPerformanceResponse;
import com.sliit.helpdesk.report.service.AuditService;
import com.sliit.helpdesk.report.service.ReportExportService;
import com.sliit.helpdesk.report.service.ReportService;
import com.sliit.helpdesk.report.service.TicketReportQuery;
import com.sliit.helpdesk.ticket.dto.TicketResponse;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST report API for ADMIN and DEPT_HEAD: summary, category load, staff
 * performance, resolution-time average, overdue SLA tickets, audit history, and export.
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'DEPT_HEAD')")
public class ReportApiController {

    private final ReportService reportService;
    private final AuditService auditService;
    private final AuthService authService;
    private final ReportExportService reportExportService;
    private final TicketReportQuery ticketReportQuery;

    public ReportApiController(
            ReportService reportService,
            AuditService auditService,
            AuthService authService,
            ReportExportService reportExportService,
            TicketReportQuery ticketReportQuery
    ) {
        this.reportService = reportService;
        this.auditService = auditService;
        this.authService = authService;
        this.reportExportService = reportExportService;
        this.ticketReportQuery = ticketReportQuery;
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

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public ReportSummaryResponse summary() {
        return reportService.summary();
    }

    @GetMapping("/by-category")
    @Transactional(readOnly = true)
    public List<CategoryCountResponse> byCategory() {
        return reportService.byCategory();
    }

    @GetMapping("/staff-performance")
    @Transactional(readOnly = true)
    public List<StaffPerformanceResponse> staffPerformance() {
        return reportService.staffPerformance();
    }

    @GetMapping("/resolution-time-avg")
    @Transactional(readOnly = true)
    public ResolutionTimeAvgResponse resolutionTimeAvg() {
        return reportService.resolutionTimeAvg();
    }

    @GetMapping("/overdue")
    @Transactional(readOnly = true)
    public List<OverdueTicketResponse> overdue() {
        return reportService.overdue();
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<AuditLogResponse> auditLogs() {
        return auditService.history().stream().map(AuditLogResponse::from).toList();
    }

    @GetMapping("/audit-logs/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AuditLogResponse auditLog(@PathVariable Long id) {
        return AuditLogResponse.from(auditService.require(id));
    }

    @PostMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<AuditLogResponse> createAuditLog(
            Authentication authentication,
            @Valid @RequestBody AuditLogRequest request
    ) {
        User user = authService.requireByEmail(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuditLogResponse.from(
                auditService.create(user, request.getAction(), request.getEntityType(), request.getEntityId())
        ));
    }

    @PutMapping("/audit-logs/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AuditLogResponse updateAuditLog(
            @PathVariable Long id,
            @Valid @RequestBody AuditLogRequest request
    ) {
        return AuditLogResponse.from(
                auditService.update(id, request.getAction(), request.getEntityType(), request.getEntityId())
        );
    }

    @DeleteMapping("/audit-logs/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteAuditLog(@PathVariable Long id) {
        auditService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Same query parameters as {@link #tickets}. Excel is streamed; PDF stays a short summary.
     * ADMIN and DEPT_HEAD are the roles allowed by the class-level rule.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPT_HEAD')")
    @Transactional(readOnly = true)
    public void export(
            @RequestParam String format,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) List<TicketStatus> status,
            @RequestParam(required = false) List<TicketPriority> priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) Long createdBy,
            Authentication authentication,
            HttpServletResponse response
    ) throws IOException {
        User user = authService.requireByEmail(authentication.getName());
        var criteria = ticketReportQuery.describe(categoryId, department, status, priority, from, to, assignedTo, createdBy);
        if (format != null && format.equalsIgnoreCase("pdf")) {
            byte[] body = reportExportService.exportPdf();
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + reportExportService.pdfFileName() + "\"");
            response.getOutputStream().write(body);
            response.flushBuffer();
            return;
        }
        if (format == null || !(format.equalsIgnoreCase("excel") || format.equalsIgnoreCase("xlsx"))) {
            throw new IllegalArgumentException("Unsupported export format: " + format + ". Use pdf or excel.");
        }
        List<Ticket> rows = ticketReportQuery.find(categoryId, department, status, priority, from, to, assignedTo, createdBy);
        reportExportService.writeExcel(response, user, criteria, rows);
    }

    @GetMapping("/tickets")
    @Transactional(readOnly = true)
    public List<TicketResponse> tickets(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) List<TicketStatus> status,
            @RequestParam(required = false) List<TicketPriority> priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) Long createdBy
    ) {
        return ticketReportQuery.find(categoryId, department, status, priority, from, to, assignedTo, createdBy)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    @GetMapping("/filter-options")
    @Transactional(readOnly = true)
    public Map<String, Object> filterOptions() {
        return ticketReportQuery.filterOptions();
    }
}
