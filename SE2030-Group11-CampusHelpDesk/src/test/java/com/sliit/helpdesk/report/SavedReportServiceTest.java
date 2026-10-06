package com.sliit.helpdesk.report;

// Saved Report Service Test is part of the campus help desk report code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.report.model.Report;
import com.sliit.helpdesk.report.repository.ReportRepository;
import com.sliit.helpdesk.report.service.SavedReportService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    private SavedReportService savedReportService;

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 9, 4, 16, 20);

    @BeforeEach
    void setUp() {
        savedReportService = new SavedReportService(reportRepository, ticketRepository, userRepository);
    }

    @Test
    void generateWithNoFiltersCountsEveryTicket() {
        stubTickets();
        stubSave();

        Report report = savedReportService.generate(admin(), "Everything", null, null, null);

        assertThat(report.getTicketCount()).isEqualTo(4);
        assertThat(report.getDepartment()).isNull();
        assertThat(report.getStatus()).isNull();
        assertThat(report.getPriority()).isNull();
    }

    @Test
    void generateAveragesResolutionHoursOverResolvedTicketsOnly() {
        stubTickets();
        stubSave();

        Report report = savedReportService.generate(admin(), "Resolved", null, TicketStatus.RESOLVED, null);

        // Two resolved tickets, 10 h and 20 h since creation.
        assertThat(report.getTicketCount()).isEqualTo(2);
        assertThat(report.getResolvedCount()).isEqualTo(2);
        assertThat(report.getAvgResolutionHours()).isEqualTo(15.0);
    }

    @Test
    void generateLeavesAverageNullWhenNothingIsResolved() {
        stubTickets();
        stubSave();

        Report report = savedReportService.generate(admin(), "Still open", null, TicketStatus.OPEN, null);

        assertThat(report.getTicketCount()).isEqualTo(1);
        assertThat(report.getResolvedCount()).isZero();
        assertThat(report.getAvgResolutionHours()).isNull();
    }

    @Test
    void generateCombinesDepartmentAndPriorityFilters() {
        stubTickets();
        stubSave();

        Report report = savedReportService.generate(admin(), "Computing highs", "faculty of computing", null,
                TicketPriority.HIGH);

        assertThat(report.getTicketCount()).isEqualTo(1);
        assertThat(report.getDepartment()).isEqualTo("faculty of computing");
    }

    @Test
    void generateRejectsBlankName() {
        assertThatThrownBy(() -> savedReportService.generate(admin(), "   ", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
        verify(reportRepository, never()).save(any());
    }

    @Test
    void deleteRejectsUnknownReport() {
        when(reportRepository.findDetailById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedReportService.delete(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
        verify(reportRepository, never()).delete(any());
    }

    @Test
    void updateNameUpdatesReportTitle() {
        Report report = new Report();
        report.setId(5L);
        report.setName("Old Name");
        when(reportRepository.findDetailById(5L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Report updated = savedReportService.updateName(5L, "New Name");

        assertThat(updated.getName()).isEqualTo("New Name");
        verify(reportRepository).save(report);
    }

    @Test
    void updateNameRejectsBlank() {
        assertThatThrownBy(() -> savedReportService.updateName(5L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
        verify(reportRepository, never()).save(any());
    }

    private void stubTickets() {
        User computing = user("Faculty of Computing");
        User facilities = user("Facilities");
        when(ticketRepository.findAllForFiltering()).thenReturn(List.of(
                ticket(computing, TicketStatus.RESOLVED, TicketPriority.LOW, CREATED.plusHours(10)),
                ticket(facilities, TicketStatus.RESOLVED, TicketPriority.MEDIUM, CREATED.plusHours(20)),
                ticket(computing, TicketStatus.OPEN, TicketPriority.HIGH, null),
                ticket(computing, TicketStatus.IN_PROGRESS, TicketPriority.MEDIUM, null)
        ));
    }

    private void stubSave() {
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static User admin() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Nirasha Perera");
        return user;
    }

    private static User user(String department) {
        User user = new User();
        user.setDepartment(department);
        return user;
    }

    private static Ticket ticket(User requester, TicketStatus status, TicketPriority priority,
                                 LocalDateTime resolvedAt) {
        Ticket ticket = new Ticket();
        ticket.setRequester(requester);
        ticket.setStatus(status);
        ticket.setPriority(priority);
        ticket.setCreatedAt(CREATED);
        ticket.setResolvedAt(resolvedAt);
        return ticket;
    }
}
