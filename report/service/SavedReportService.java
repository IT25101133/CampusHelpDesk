package com.sliit.helpdesk.report.service;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.report.model.Report;
import com.sliit.helpdesk.report.repository.ReportRepository;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Generates and stores filtered ticket reports so admins can revisit or discard them later.
 */
@Service
public class SavedReportService {

    private final ReportRepository reportRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public SavedReportService(
            ReportRepository reportRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository
    ) {
        this.reportRepository = reportRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    /**
     * Department values that actually appear on user records, for the filter dropdown.
     */
    public List<String> departments() {
        return userRepository.findDistinctDepartments();
    }

    @Transactional
    public Report generate(
            User createdBy,
            String name,
            String department,
            TicketStatus status,
            TicketPriority priority
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Report name is required");
        }
        String scopedDepartment = blankToNull(department);
        List<Ticket> matches = match(scopedDepartment, status, priority);
        List<Ticket> resolved = matches.stream().filter(SavedReportService::hasResolutionWindow).toList();

        Report report = new Report();
        report.setName(name.trim());
        report.setDepartment(scopedDepartment);
        report.setStatus(status);
        report.setPriority(priority);
        report.setCreatedBy(createdBy);
        report.setTicketCount(matches.size());
        report.setResolvedCount(resolved.size());
        report.setAvgResolutionHours(averageResolutionHours(resolved));
        return reportRepository.save(report);
    }

    public List<Report> history() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    public Report require(Long id) {
        return reportRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }

    /**
     * Re-runs a saved report's filters so the detail view can list the tickets behind the totals.
     */
    public List<Ticket> ticketsFor(Report report) {
        return match(report.getDepartment(), report.getStatus(), report.getPriority());
    }

    @Transactional
    public Report updateName(Long id, String name) {
        return updateName(null, id, name);
    }

    /**
     * Rename keeps the stored filters. A null actor is only used by existing unit tests.
     */
    @Transactional
    public Report updateName(User actor, Long id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Report name is required");
        }
        Report report = require(id);
        if (actor != null) {
            assertCanChange(actor, report);
            report.setUpdatedBy(actor);
        }
        report.setName(name.trim());
        report.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    /**
     * Recomputes the saved totals from the new filters. createdBy and createdAt stay as they were.
     */
    @Transactional
    public Report update(User actor, Long id, String name, String department, TicketStatus status, TicketPriority priority) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Report name is required");
        }
        Report report = require(id);
        assertCanChange(actor, report);
        String scopedDepartment = blankToNull(department);
        List<Ticket> matches = match(scopedDepartment, status, priority);
        List<Ticket> resolved = matches.stream().filter(SavedReportService::hasResolutionWindow).toList();
        report.setName(name.trim());
        report.setDepartment(scopedDepartment);
        report.setStatus(status);
        report.setPriority(priority);
        report.setTicketCount(matches.size());
        report.setResolvedCount(resolved.size());
        report.setAvgResolutionHours(averageResolutionHours(resolved));
        report.setUpdatedBy(actor);
        report.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    @Transactional
    public void delete(Long id) {
        reportRepository.delete(require(id));
    }

    /**
     * Hard-deletes the row. Only an admin or the user who generated the report may do this.
     */
    @Transactional
    public void delete(User actor, Long id) {
        Report report = require(id);
        assertCanChange(actor, report);
        reportRepository.delete(report);
    }

    private void assertCanChange(User actor, Report report) {
        if (actor == null) {
            throw new IllegalArgumentException("You cannot change this report.");
        }
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        User creator = report.getCreatedBy();
        if (creator != null && creator.getId() != null && creator.getId().equals(actor.getId())) {
            return;
        }
        throw new IllegalArgumentException("You cannot change this report.");
    }

    private List<Ticket> match(String department, TicketStatus status, TicketPriority priority) {
        return ticketRepository.findAllForFiltering().stream()
                .filter(ticket -> status == null || ticket.getStatus() == status)
                .filter(ticket -> priority == null || ticket.getPriority() == priority)
                .filter(ticket -> department == null || inDepartment(ticket, department))
                .toList();
    }

    private static boolean inDepartment(Ticket ticket, String department) {
        User requester = ticket.getRequester();
        return requester != null && department.equalsIgnoreCase(requester.getDepartment());
    }

    /**
     * Tickets still in flight have no resolution window and must not drag the average down.
     */
    private static boolean hasResolutionWindow(Ticket ticket) {
        return ticket.getCreatedAt() != null
                && ticket.getResolvedAt() != null
                && !ticket.getResolvedAt().isBefore(ticket.getCreatedAt());
    }

    private static Double averageResolutionHours(List<Ticket> resolved) {
        if (resolved.isEmpty()) {
            return null;
        }
        double totalHours = 0;
        for (Ticket ticket : resolved) {
            totalHours += ReportService.hoursBetween(ticket.getCreatedAt(), ticket.getResolvedAt());
        }
        return ReportService.roundHours(totalHours / resolved.size());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
