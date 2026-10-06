package com.sliit.helpdesk.report;

// Report Service Test is part of the campus help desk report code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.notification.service.NotificationService;
import com.sliit.helpdesk.report.dto.DashboardStats;
import com.sliit.helpdesk.report.dto.OverdueTicketResponse;
import com.sliit.helpdesk.report.dto.ResolutionStats;
import com.sliit.helpdesk.report.dto.ResolutionTimeAvgResponse;
import com.sliit.helpdesk.report.service.ReportService;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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

    @Test
    void resolutionTimeAvgUsesKnownCreatedAndResolvedTimestamps() {
        LocalDateTime t0 = LocalDateTime.of(2026, 9, 1, 10, 0);
        Ticket fourHours = ticket(1L, TicketStatus.RESOLVED, t0, t0.plusHours(4), category("IT Support", 24));
        Ticket eightHours = ticket(2L, TicketStatus.RESOLVED, t0, t0.plusHours(8), category("Facilities", 48));
        Ticket stillOpen = ticket(3L, TicketStatus.OPEN, t0, null, category("IT Support", 24));

        ResolutionTimeAvgResponse avg = reportService.resolutionTimeAvg(List.of(fourHours, eightHours, stillOpen));

        assertThat(avg.getResolvedCount()).isEqualTo(2L);
        assertThat(avg.getAverageHours()).isEqualTo(6.0);
    }

    @Test
    void resolutionStatsBreaksDownOverallPriorityAndCategory() {
        LocalDateTime t0 = LocalDateTime.of(2026, 9, 1, 10, 0);
        Category it = category("IT Support", 24);
        Category facilities = category("Facilities", 48);
        Ticket highIt = ticket(1L, TicketStatus.RESOLVED, t0, t0.plusHours(4), it);
        highIt.setPriority(TicketPriority.HIGH);
        Ticket mediumFacilities = ticket(2L, TicketStatus.CLOSED, t0, t0.plusHours(8), facilities);
        mediumFacilities.setPriority(TicketPriority.MEDIUM);
        Ticket stillOpen = ticket(3L, TicketStatus.OPEN, t0, null, it);
        stillOpen.setPriority(TicketPriority.HIGH);
        Ticket resolvedWithoutTimestamp = ticket(4L, TicketStatus.RESOLVED, t0, null, it);
        resolvedWithoutTimestamp.setPriority(TicketPriority.LOW);

        ResolutionStats stats = reportService.resolutionStats(
                List.of(highIt, mediumFacilities, stillOpen, resolvedWithoutTimestamp)
        );

        assertThat(stats.getResolvedCount()).isEqualTo(2L);
        assertThat(stats.getOverallAverageHours()).isEqualTo(6.0);
        assertThat(stats.getByPriority())
                .extracting(ResolutionStats.GroupAverage::getName)
                .containsExactly("LOW", "MEDIUM", "HIGH", "CRITICAL");
        assertThat(averageNamed(stats.getByPriority(), "HIGH")).isEqualTo(4.0);
        assertThat(averageNamed(stats.getByPriority(), "MEDIUM")).isEqualTo(8.0);
        assertThat(averageNamed(stats.getByPriority(), "LOW")).isEqualTo(0.0);
        assertThat(stats.getByCategory()).extracting(ResolutionStats.GroupAverage::getName)
                .containsExactly("Facilities", "IT Support");
        assertThat(averageNamed(stats.getByCategory(), "IT Support")).isEqualTo(4.0);
        assertThat(averageNamed(stats.getByCategory(), "Facilities")).isEqualTo(8.0);
    }

    @Test
    void resolutionTimeAvgIsZeroWhenNoResolvedTimestampsExist() {
        Ticket open = ticket(4L, TicketStatus.OPEN, LocalDateTime.of(2026, 9, 10, 8, 0), null, category("IT Support", 24));

        ResolutionTimeAvgResponse avg = reportService.resolutionTimeAvg(List.of(open));

        assertThat(avg.getResolvedCount()).isZero();
        assertThat(avg.getAverageHours()).isZero();
    }

    @Test
    void overdueIncludesUnresolvedTicketsPastCategorySlaHours() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 11, 12, 0);
        Category it = category("IT Support", 24);
        Category facilities = category("Facilities", 48);

        Ticket overdueOpen = ticket(10L, TicketStatus.OPEN, now.minusHours(48), null, it);
        overdueOpen.setTitle("VPN down");
        Ticket overduePending = ticket(11L, TicketStatus.IN_PROGRESS, now.minusHours(25), null, it);
        overduePending.setTitle("Mailbox full");
        Ticket withinSla = ticket(12L, TicketStatus.OPEN, now.minusHours(10), null, it);
        Ticket resolvedPastSla = ticket(13L, TicketStatus.RESOLVED, now.minusHours(100), now.minusHours(90), it);
        Ticket facilitiesNotYetDue = ticket(14L, TicketStatus.OPEN, now.minusHours(47), null, facilities);

        List<OverdueTicketResponse> overdue = reportService.overdue(
                List.of(overdueOpen, overduePending, withinSla, resolvedPastSla, facilitiesNotYetDue),
                now
        );

        assertThat(overdue).extracting(OverdueTicketResponse::getId).containsExactly(10L, 11L);
        assertThat(overdue.get(0).getHoursOverdue()).isEqualTo(24.0);
        assertThat(overdue.get(1).getHoursOverdue()).isEqualTo(1.0);
        assertThat(overdue.get(0).getSlaHours()).isEqualTo(24);
        assertThat(reportService.isOverdue(withinSla, now)).isFalse();
        assertThat(reportService.isOverdue(resolvedPastSla, now)).isFalse();
        assertThat(reportService.isOverdue(facilitiesNotYetDue, now)).isFalse();
    }

    private static double averageNamed(List<ResolutionStats.GroupAverage> rows, String name) {
        return rows.stream()
                .filter(row -> name.equals(row.getName()))
                .map(ResolutionStats.GroupAverage::getAverageHours)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing group " + name));
    }

    private static Category category(String name, int slaHours) {
        Category category = new Category();
        category.setName(name);
        category.setSlaHours(slaHours);
        return category;
    }

    private static Ticket ticket(
            Long id,
            TicketStatus status,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt,
            Category category
    ) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle("Ticket " + id);
        ticket.setDescription("Fixture");
        ticket.setStatus(status);
        ticket.setCreatedAt(createdAt);
        ticket.setResolvedAt(resolvedAt);
        ticket.setCategory(category);
        return ticket;
    }
}
