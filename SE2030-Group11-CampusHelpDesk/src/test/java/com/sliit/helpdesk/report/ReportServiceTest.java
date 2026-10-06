package com.sliit.helpdesk.report;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.notification.service.NotificationService;
import com.sliit.helpdesk.report.dto.DashboardStats;
import com.sliit.helpdesk.report.service.ReportService;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private NotificationService notificationService;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(ticketRepository, notificationService);
    }

    @Test
    void buildAggregatesCampusWideCountsForStaff() {
        User staff = new User();
        staff.setRole(Role.STAFF);
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.RESOLVED)).thenReturn(3L);
        when(ticketRepository.countByStatus(TicketStatus.CLOSED)).thenReturn(0L);
        when(ticketRepository.count()).thenReturn(6L);
        when(notificationService.unreadCount(staff)).thenReturn(4L);
        when(ticketRepository.countGroupedByStatus()).thenReturn(List.of());
        when(ticketRepository.countGroupedByPriority()).thenReturn(List.of());
        when(ticketRepository.countGroupedByCategory()).thenReturn(List.of());

        DashboardStats stats = reportService.build(staff);

        assertThat(stats.getTotalTickets()).isEqualTo(6L);
        assertThat(stats.getOpenTickets()).isEqualTo(2L);
        assertThat(stats.getUnreadNotifications()).isEqualTo(4L);
    }
}
