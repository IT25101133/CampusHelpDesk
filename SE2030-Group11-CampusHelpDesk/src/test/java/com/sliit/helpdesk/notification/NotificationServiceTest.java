package com.sliit.helpdesk.notification;

// Notification Service Test is part of the campus help desk notification code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.category.service.CategoryService;
import com.sliit.helpdesk.notification.model.Notification;
import com.sliit.helpdesk.notification.repository.NotificationRepository;
import com.sliit.helpdesk.notification.service.CommentService;
import com.sliit.helpdesk.notification.service.NotificationService;
import com.sliit.helpdesk.report.service.AuditService;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketAttachmentRepository;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import com.sliit.helpdesk.ticket.service.TicketFactory;
import com.sliit.helpdesk.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private CommentService commentService;
    @Mock
    private TicketAttachmentRepository attachmentRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AuthService authService;
    @Mock
    private AuditService auditService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, ticketRepository);
    }

    @Test
    void notifyPersistsUnreadAssignment() {
        User staff = new User();
        staff.setId(2L);
        staff.setEmail("staff@sliit.lk");
        Ticket ticket = new Ticket();
        ticket.setId(5L);

        notificationService.notify(staff, "Ticket assigned to you: HD-5", ticket);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo(staff);
        assertThat(captor.getValue().getMessage()).isEqualTo("Ticket assigned to you: HD-5");
        assertThat(captor.getValue().isRead()).isFalse();
        assertThat(captor.getValue().getTicket()).isEqualTo(ticket);
    }

    @Test
    void onStatusChangedInsertsNotificationRow() {
        User requester = requester();
        Ticket ticket = ticket(requester);

        notificationService.onStatusChanged(ticket, TicketStatus.OPEN, TicketStatus.IN_PROGRESS);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipient()).isEqualTo(requester);
        assertThat(saved.getTicket()).isEqualTo(ticket);
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getMessage()).isEqualTo("Ticket HD-12 is now IN PROGRESS");
    }

    @Test
    void onAssignedNotifiesNewAssignee() {
        User requester = requester();
        User staff = new User();
        staff.setId(2L);
        staff.setRole(Role.STAFF);
        staff.setEmail("staff@sliit.lk");
        staff.setFullName("Campus Staff");
        Ticket ticket = ticket(requester);

        notificationService.onAssigned(ticket, staff);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo(staff);
        assertThat(captor.getValue().getMessage()).contains("assigned to you");
        assertThat(captor.getValue().getLink()).isEqualTo("/tickets/12");
        assertThat(captor.getValue().isRead()).isFalse();
    }

    @Test
    void onCreatedNotifiesAdminRoleAfterCommit() {
        User requester = requester();
        Ticket ticket = ticket(requester);
        when(ticketRepository.findById(12L)).thenReturn(Optional.of(ticket));
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            notificationService.onCreated(ticket);
            verify(notificationRepository, org.mockito.Mockito.never()).save(any());
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).anySatisfy(saved -> {
            assertThat(saved.getRecipient()).isEqualTo(requester);
            assertThat(saved.getMessage()).contains("was submitted");
            assertThat(saved.isRead()).isFalse();
        });
        assertThat(captor.getAllValues()).anySatisfy(saved -> {
            assertThat(saved.getRecipient()).isNull();
            assertThat(saved.getRecipientRole()).isEqualTo("ADMIN");
            assertThat(saved.isRead()).isFalse();
        });
    }

    @Test
    void highPriorityNotificationIsMarkedUrgent() {
        User staff = new User();
        staff.setId(2L);
        staff.setEmail("staff@sliit.lk");
        Ticket ticket = new Ticket();
        ticket.setId(9L);
        ticket.setPriority(TicketPriority.HIGH);

        notificationService.notify(staff, "Network outage", ticket);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("[URGENT] Network outage");
    }

    @Test
    void ticketStatusChangeCreatesNotificationRow() {
        User requester = requester();
        User staff = new User();
        staff.setId(2L);
        staff.setRole(Role.STAFF);
        staff.setEmail("staff@sliit.lk");
        Ticket ticket = ticket(requester);
        when(ticketRepository.findById(12L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketService ticketService = new TicketService(
                ticketRepository,
                commentService,
                attachmentRepository,
                categoryService,
                notificationService,
                List.of(notificationService),
                authService,
                auditService,
                new TicketFactory(),
                Path.of(System.getProperty("java.io.tmpdir"), "helpdesk-uploads").toString()
        );

        ticketService.updateStatus(12L, staff, TicketStatus.RESOLVED);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipient()).isEqualTo(requester);
        assertThat(saved.getTicket()).isEqualTo(ticket);
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getMessage()).contains("RESOLVED");
        assertThat(saved.getMessage()).contains("Confirm and close");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    private static User requester() {
        User requester = new User();
        requester.setId(1L);
        requester.setRole(Role.STUDENT);
        requester.setEmail("student@sliit.lk");
        requester.setFullName("Demo Student");
        return requester;
    }

    private static Ticket ticket(User requester) {
        Ticket ticket = new Ticket();
        ticket.setId(12L);
        ticket.setTitle("Campus Wi-Fi");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setRequester(requester);
        return ticket;
    }
}
