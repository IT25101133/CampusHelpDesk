package com.sliit.helpdesk.report.service;

// Report Service is part of the campus help desk service code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.notification.service.NotificationService;
import com.sliit.helpdesk.report.dto.CategoryCountResponse;
import com.sliit.helpdesk.report.dto.DashboardStats;
import com.sliit.helpdesk.report.dto.OverdueTicketResponse;
import com.sliit.helpdesk.report.dto.ReportSummaryResponse;
import com.sliit.helpdesk.report.dto.ResolutionStats;
import com.sliit.helpdesk.report.dto.ResolutionTimeAvgResponse;
import com.sliit.helpdesk.report.dto.StaffPerformanceResponse;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;

    public ReportService(TicketRepository ticketRepository, NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.notificationService = notificationService;
    }

    public DashboardStats build(User user) {
        DashboardStats stats = new DashboardStats();
        stats.setOpenTickets(ticketRepository.countByStatus(TicketStatus.OPEN));
        stats.setInProgressTickets(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS));
        stats.setResolvedTickets(ticketRepository.countByStatus(TicketStatus.RESOLVED));
        stats.setClosedTickets(ticketRepository.countByStatus(TicketStatus.CLOSED));
        stats.setTotalTickets(ticketRepository.count());
        stats.setUnreadNotifications(notificationService.unreadCount(user));
        stats.setByStatus(toLabelMap(ticketRepository.countGroupedByStatus()));
        stats.setByPriority(toLabelMap(ticketRepository.countGroupedByPriority()));
        stats.setByCategory(toLabelMap(ticketRepository.countGroupedByCategory()));

        if (user.getRole() == Role.STUDENT) {
            List<Ticket> mine = ticketRepository.findByRequesterOrderByCreatedAtDesc(user);
            stats.setTotalTickets(mine.size());
            stats.setOpenTickets(mine.stream().filter(t -> t.getStatus() == TicketStatus.OPEN).count());
            stats.setInProgressTickets(mine.stream().filter(t -> t.getStatus() == TicketStatus.IN_PROGRESS).count());
            stats.setResolvedTickets(mine.stream().filter(t -> t.getStatus() == TicketStatus.RESOLVED).count());
            stats.setClosedTickets(mine.stream().filter(t -> t.getStatus() == TicketStatus.CLOSED).count());
        }
        return stats;
    }

    public ReportSummaryResponse summary() {
        ReportSummaryResponse summary = new ReportSummaryResponse();
        long open = ticketRepository.countByStatus(TicketStatus.OPEN);
        long pending = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long resolved = ticketRepository.countByStatus(TicketStatus.RESOLVED)
                + ticketRepository.countByStatus(TicketStatus.CLOSED);
        summary.setOpenTickets(open);
        summary.setPendingTickets(pending);
        summary.setResolvedTickets(resolved);
        summary.setTotalTickets(ticketRepository.count());
        return summary;
    }

    public List<CategoryCountResponse> byCategory() {
        List<CategoryCountResponse> results = new ArrayList<>();
        for (Object[] row : ticketRepository.countGroupedByCategoryWithId()) {
            CategoryCountResponse item = new CategoryCountResponse();
            item.setCategoryId(toLong(row[0]));
            item.setCategory(String.valueOf(row[1]));
            item.setTicketCount(toLong(row[2]));
            results.add(item);
        }
        return results;
    }

    public List<StaffPerformanceResponse> staffPerformance() {
        return staffPerformance(ticketRepository.findAllForReporting());
    }

    public List<StaffPerformanceResponse> staffPerformance(List<Ticket> tickets) {
        Map<Long, StaffPerformanceResponse> byStaff = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            if (!isCompleted(ticket) || ticket.getAssignee() == null || ticket.getAssignee().getId() == null) {
                continue;
            }
            User staff = ticket.getAssignee();
            StaffPerformanceResponse row = byStaff.computeIfAbsent(staff.getId(), id -> {
                StaffPerformanceResponse item = new StaffPerformanceResponse();
                item.setStaffId(id);
                item.setStaffName(staff.getFullName());
                return item;
            });
            row.setTicketsResolved(row.getTicketsResolved() + 1);
        }
        List<StaffPerformanceResponse> results = new ArrayList<>(byStaff.values());
        results.sort(Comparator.comparingLong(StaffPerformanceResponse::getTicketsResolved).reversed());
        return results;
    }

    /**
     * Campus-wide resolution hours for RESOLVED/CLOSED tickets that have {@code resolvedAt}.
     * Kept separate from {@link #build} so dashboard count cards stay unchanged.
     */
    public ResolutionStats resolutionStats() {
        return resolutionStats(ticketRepository.findAllForReporting());
    }

    public ResolutionStats resolutionStats(List<Ticket> tickets) {
        List<Ticket> resolved = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (!isCompleted(ticket) || ticket.getCreatedAt() == null || ticket.getResolvedAt() == null) {
                continue;
            }
            if (ticket.getResolvedAt().isBefore(ticket.getCreatedAt())) {
                continue;
            }
            resolved.add(ticket);
        }

        ResolutionStats stats = new ResolutionStats();
        stats.setResolvedCount(resolved.size());
        stats.setOverallAverageHours(averageHours(resolved));

        List<ResolutionStats.GroupAverage> byPriority = new ArrayList<>();
        for (TicketPriority priority : TicketPriority.values()) {
            List<Ticket> group = resolved.stream()
                    .filter(ticket -> ticket.getPriority() == priority)
                    .toList();
            byPriority.add(new ResolutionStats.GroupAverage(priority.name(), averageHours(group), group.size()));
        }
        stats.setByPriority(byPriority);

        Map<String, List<Ticket>> byCategoryName = new LinkedHashMap<>();
        for (Ticket ticket : resolved) {
            byCategoryName.computeIfAbsent(categoryName(ticket), key -> new ArrayList<>()).add(ticket);
        }
        List<ResolutionStats.GroupAverage> byCategory = new ArrayList<>();
        for (Map.Entry<String, List<Ticket>> entry : byCategoryName.entrySet()) {
            byCategory.add(new ResolutionStats.GroupAverage(
                    entry.getKey(),
                    averageHours(entry.getValue()),
                    entry.getValue().size()
            ));
        }
        byCategory.sort(Comparator.comparing(ResolutionStats.GroupAverage::getName, String.CASE_INSENSITIVE_ORDER));
        stats.setByCategory(byCategory);
        return stats;
    }

    public ResolutionTimeAvgResponse resolutionTimeAvg() {
        return resolutionTimeAvg(ticketRepository.findAllForReporting());
    }

    /**
     * Average hours between {@code createdAt} and {@code resolvedAt} for tickets that have both timestamps.
     */
    public ResolutionTimeAvgResponse resolutionTimeAvg(List<Ticket> tickets) {
        double totalHours = 0;
        long count = 0;
        for (Ticket ticket : tickets) {
            if (ticket.getCreatedAt() == null || ticket.getResolvedAt() == null) {
                continue;
            }
            if (ticket.getResolvedAt().isBefore(ticket.getCreatedAt())) {
                continue;
            }
            totalHours += hoursBetween(ticket.getCreatedAt(), ticket.getResolvedAt());
            count++;
        }
        ResolutionTimeAvgResponse response = new ResolutionTimeAvgResponse();
        response.setResolvedCount(count);
        response.setAverageHours(count == 0 ? 0 : roundHours(totalHours / count));
        return response;
    }

    public List<OverdueTicketResponse> overdue() {
        return overdue(ticketRepository.findAllForReporting(), LocalDateTime.now());
    }

    /**
     * Unresolved tickets whose age exceeds their category's {@code sla_hours}.
     */
    public List<OverdueTicketResponse> overdue(List<Ticket> tickets, LocalDateTime now) {
        List<OverdueTicketResponse> overdue = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (!isOverdue(ticket, now)) {
                continue;
            }
            int slaHours = ticket.getCategory().getSlaHours();
            LocalDateTime dueAt = ticket.getCreatedAt().plusHours(slaHours);
            OverdueTicketResponse item = new OverdueTicketResponse();
            item.setId(ticket.getId());
            item.setTicketNumber(ticket.getTicketNumber());
            item.setTitle(ticket.getTitle());
            item.setStatus(ticket.getStatus());
            item.setCategory(ticket.getCategory().getName());
            item.setSlaHours(slaHours);
            item.setCreatedAt(ticket.getCreatedAt());
            item.setDueAt(dueAt);
            item.setHoursOverdue(roundHours(hoursBetween(dueAt, now)));
            overdue.add(item);
        }
        overdue.sort(Comparator.comparing(OverdueTicketResponse::getHoursOverdue).reversed());
        return overdue;
    }

    public boolean isOverdue(Ticket ticket, LocalDateTime now) {
        if (ticket == null || isCompleted(ticket) || ticket.getCreatedAt() == null || ticket.getCategory() == null) {
            return false;
        }
        int slaHours = ticket.getCategory().getSlaHours();
        LocalDateTime dueAt = ticket.getCreatedAt().plusHours(Math.max(slaHours, 0));
        return dueAt.isBefore(now);
    }

    private boolean isCompleted(Ticket ticket) {
        return ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED;
    }

    private static String categoryName(Ticket ticket) {
        if (ticket.getCategory() == null || ticket.getCategory().getName() == null || ticket.getCategory().getName().isBlank()) {
            return "Uncategorized";
        }
        return ticket.getCategory().getName();
    }

    private static double averageHours(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return 0;
        }
        double totalHours = 0;
        for (Ticket ticket : tickets) {
            totalHours += hoursBetween(ticket.getCreatedAt(), ticket.getResolvedAt());
        }
        return roundHours(totalHours / tickets.size());
    }

    private Map<String, Long> toLabelMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = String.valueOf(row[0]).replace('_', ' ');
            map.put(key, toLong(row[1]));
        }
        return map;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(String.valueOf(value));
    }

    static double hoursBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toMillis() / 3_600_000.0;
    }

    static double roundHours(double hours) {
        return Math.round(hours * 100.0) / 100.0;
    }
}
