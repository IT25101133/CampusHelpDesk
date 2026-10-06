package com.sliit.helpdesk.report.service;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.notification.service.NotificationService;
import com.sliit.helpdesk.report.dto.DashboardStats;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;

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

    private Map<String, Long> toLabelMap(java.util.List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = String.valueOf(row[0]).replace('_', ' ');
            Long value = (Long) row[1];
            map.put(key, value);
        }
        return map;
    }
}
