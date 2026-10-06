package com.sliit.helpdesk.ticket;

// Ticket Service Test is part of the campus help desk ticket code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.category.service.CategoryService;
import com.sliit.helpdesk.notification.service.CommentService;
import com.sliit.helpdesk.notification.service.NotificationService;
import com.sliit.helpdesk.notification.model.Notification;
import com.sliit.helpdesk.notification.repository.NotificationRepository;
import com.sliit.helpdesk.report.service.AuditService;
import com.sliit.helpdesk.ticket.dto.TicketForm;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketAttachment;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private CommentService commentService;
    @Mock private TicketAttachmentRepository attachmentRepository;
    @Mock private CategoryService categoryService;
    @Mock private NotificationService notificationService;
    @Mock private AuthService authService;
    @Mock private AuditService auditService;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
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
    }

    @Test
    void createTicketSetsStatusToOpen() {
        User student = user(1L, Role.STUDENT);
        Category category = new Category();
        category.setName("IT Support");
        category.setSlaHours(48);
        TicketForm form = new TicketForm();
        form.setTitle("LMS down");
        form.setDescription("Cannot open CourseWeb");
        form.setCategoryId(4L);
        form.setPriority(TicketPriority.HIGH);
        when(categoryService.requireActive(4L)).thenReturn(category);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(4L);
            return ticket;
        });

        Ticket ticket = ticketService.create(student, form);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getAssignee()).isNull();
        assertThat(ticket.getSubmitterRole()).isEqualTo(Role.STUDENT);
        assertThat(ticket.getTicketNumber()).isEqualTo("HD-4");
        assertThat(ticket.getRequester()).isEqualTo(student);
        assertThat(ticket.getTitle()).isEqualTo("LMS down");
        assertThat(ticket.getCategory()).isEqualTo(category);
        assertThat(ticket.getSlaDueAt()).isNotNull();
    }

    @Test
    void createRejectsMissingDepartment() {
        User student = user(1L, Role.STUDENT);
        TicketForm form = new TicketForm();
        form.setTitle("LMS down");
        form.setDescription("Cannot open CourseWeb");

        assertThatThrownBy(() -> ticketService.create(student, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Please select a department");
    }

    @Test
    void staffCanChangeTicketCategory() {
        User staff = user(2L, Role.STAFF);
        Category current = new Category();
        current.setId(4L);
        current.setName("IT Support");
        current.setDepartment("IT Services");
        current.setSlaHours(24);
        Category next = new Category();
        next.setId(5L);
        next.setName("Facilities");
        next.setDepartment("Facilities");
        next.setSlaHours(48);
        Ticket open = ticket(3L, TicketStatus.OPEN, staff);
        open.setCategory(current);
        when(ticketRepository.findById(3L)).thenReturn(Optional.of(open));
        when(categoryService.requireActive(5L)).thenReturn(next);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.sliit.helpdesk.ticket.dto.TicketUpdateRequest request = new com.sliit.helpdesk.ticket.dto.TicketUpdateRequest();
        request.setCategoryId(5L);
        Ticket updated = ticketService.update(3L, staff, request);

        assertThat(updated.getCategory()).isEqualTo(next);
        assertThat(updated.getCategory().getDepartment()).isEqualTo("Facilities");
    }

    @Test
    void reopenOnlyWorksOnClosedTickets() {
        User staff = user(2L, Role.STAFF);
        User assignee = user(3L, Role.STAFF);
        Ticket closed = ticket(8L, TicketStatus.CLOSED, staff);
        closed.setAssignee(assignee);
        closed.setResolvedAt(LocalDateTime.now().minusDays(2));
        when(ticketRepository.findById(8L)).thenReturn(Optional.of(closed));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> ticketService.reopen(8L, staff, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");

        Ticket reopened = ticketService.reopen(8L, staff, "Issue came back");

        assertThat(reopened.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(reopened.getAssignee()).isEqualTo(assignee);
        assertThat(reopened.getResolvedAt()).isNull();
        verify(auditService).log(eq(staff), contains("Issue came back"), eq("TICKET"), eq(8L));

        Ticket inProgress = ticket(9L, TicketStatus.IN_PROGRESS, staff);
        when(ticketRepository.findById(9L)).thenReturn(Optional.of(inProgress));

        assertThatThrownBy(() -> ticketService.reopen(9L, staff, "again"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void studentReopenKeepsAssigneeInsideSevenDaysAndStopsAfter() {
        User student = user(1L, Role.STUDENT);
        User assignee = user(2L, Role.STAFF);
        Ticket recent = ticket(8L, TicketStatus.CLOSED, student);
        recent.setAssignee(assignee);
        recent.setResolvedAt(LocalDateTime.now().minusDays(3));
        when(ticketRepository.findById(8L)).thenReturn(Optional.of(recent));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket reopened = ticketService.reopen(8L, student, "Still happening");

        assertThat(reopened.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(reopened.getAssignee()).isSameAs(assignee);

        Ticket old = ticket(10L, TicketStatus.CLOSED, student);
        old.setAssignee(assignee);
        old.setResolvedAt(LocalDateTime.now().minusDays(8));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(old));

        assertThatThrownBy(() -> ticketService.reopen(10L, student, "Too late"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("new ticket");

        User staff = user(4L, Role.STAFF);
        ticketService.reopen(10L, staff, "Staff follow-up");
        assertThat(old.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(old.getAssignee()).isSameAs(assignee);
    }

    @Test
    void deleteOnlyWorksOnTicketsNotYetInProgress() {
        User student = user(1L, Role.STUDENT);
        Ticket open = ticket(3L, TicketStatus.OPEN, student);
        when(ticketRepository.findById(3L)).thenReturn(Optional.of(open));

        ticketService.delete(3L, student);

        verify(ticketRepository).delete(open);

        Ticket inProgress = ticket(4L, TicketStatus.IN_PROGRESS, student);
        when(ticketRepository.findById(4L)).thenReturn(Optional.of(inProgress));

        assertThatThrownBy(() -> ticketService.delete(4L, student))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approve");

        inProgress.setDeleteRequested(true);
        inProgress.setDeleteApproved(true);
        ticketService.delete(4L, student);
        verify(ticketRepository).delete(inProgress);

        User admin = user(9L, Role.ADMIN);
        ticketService.delete(4L, admin);
        verify(ticketRepository, times(2)).delete(inProgress);
    }

    @Test
    void studentCannotSetStaffControlledStatus() {
        User student = user(1L, Role.STUDENT);
        Ticket open = ticket(1L, TicketStatus.OPEN, student);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(open));

        assertThatThrownBy(() -> ticketService.updateStatus(1L, student, TicketStatus.OPEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot");
        assertThatThrownBy(() -> ticketService.updateStatus(1L, student, TicketStatus.IN_PROGRESS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot");
    }

    @Test
    void studentCanCloseOwnTicket() {
        User student = user(1L, Role.STUDENT);
        Ticket open = ticket(1L, TicketStatus.OPEN, student);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(open));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.updateStatus(1L, student, TicketStatus.CLOSED);

        assertThat(open.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(open.getResolvedAt()).isNotNull();
    }

    @Test
    void updateStatusSetsResolvedAtOnFirstResolve() {
        User student = user(1L, Role.STUDENT);
        User staff = user(2L, Role.STAFF);
        Ticket open = ticket(6L, TicketStatus.OPEN, student);
        when(ticketRepository.findById(6L)).thenReturn(Optional.of(open));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.updateStatus(6L, staff, TicketStatus.RESOLVED);

        assertThat(open.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(open.getResolvedAt()).isNotNull();
    }

    @Test
    void updateStatusDoesNotOverwriteExistingResolvedAtWhenClosing() {
        User student = user(1L, Role.STUDENT);
        User staff = user(2L, Role.STAFF);
        LocalDateTime firstResolved = LocalDateTime.of(2026, 9, 10, 12, 0);
        Ticket resolved = ticket(6L, TicketStatus.RESOLVED, student);
        resolved.setResolvedAt(firstResolved);
        when(ticketRepository.findById(6L)).thenReturn(Optional.of(resolved));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.staffCloseResolved(6L, staff, "Confirmed by phone");

        assertThat(resolved.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(resolved.getResolvedAt()).isEqualTo(firstResolved);
    }

    @Test
    void updateStatusClearsResolvedAtWhenMovedBackToOpenOrInProgress() {
        User student = user(1L, Role.STUDENT);
        User staff = user(2L, Role.STAFF);
        Ticket resolved = ticket(6L, TicketStatus.RESOLVED, student);
        resolved.setResolvedAt(LocalDateTime.of(2026, 9, 10, 12, 0));
        when(ticketRepository.findById(6L)).thenReturn(Optional.of(resolved));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.updateStatus(6L, staff, TicketStatus.OPEN);
        assertThat(resolved.getResolvedAt()).isNull();

        resolved.setStatus(TicketStatus.RESOLVED);
        resolved.setResolvedAt(LocalDateTime.of(2026, 9, 11, 9, 0));
        resolved.setAssignee(staff);
        ticketService.updateStatus(6L, staff, TicketStatus.IN_PROGRESS);
        assertThat(resolved.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(resolved.getResolvedAt()).isNull();
    }

    @Test
    void updateStatusRecomputesResolvedAtAfterReopenAndResolve() {
        User student = user(1L, Role.STUDENT);
        User staff = user(2L, Role.STAFF);
        Ticket ticket = ticket(6L, TicketStatus.RESOLVED, student);
        ticket.setResolvedAt(LocalDateTime.of(2026, 9, 10, 12, 0));
        when(ticketRepository.findById(6L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.updateStatus(6L, staff, TicketStatus.OPEN);
        assertThat(ticket.getResolvedAt()).isNull();

        ticketService.updateStatus(6L, staff, TicketStatus.RESOLVED);
        assertThat(ticket.getResolvedAt()).isNotNull();
    }

    @Test
    void updateStatusCreatesNotificationRow() {
        User student = user(1L, Role.STUDENT);
        student.setEmail("student@sliit.lk");
        User staff = user(2L, Role.STAFF);
        Ticket open = ticket(6L, TicketStatus.OPEN, student);
        open.setAssignee(staff);
        NotificationRepository notificationRepository = org.mockito.Mockito.mock(NotificationRepository.class);
        NotificationService realNotifications = new NotificationService(notificationRepository, ticketRepository);
        ticketService = new TicketService(
                ticketRepository,
                commentService,
                attachmentRepository,
                categoryService,
                realNotifications,
                List.of(realNotifications),
                authService,
                auditService,
                new TicketFactory(),
                Path.of(System.getProperty("java.io.tmpdir"), "helpdesk-uploads").toString()
        );
        when(ticketRepository.findById(6L)).thenReturn(Optional.of(open));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.updateStatus(6L, staff, TicketStatus.IN_PROGRESS);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getRecipient).containsExactly(student, staff);
        assertThat(captor.getAllValues()).allMatch(row -> !row.isRead() && row.getTicket() == open);
        assertThat(captor.getAllValues().get(0).getMessage()).contains("IN PROGRESS");
    }

    @Test
    void escalateBumpsPriorityAndAssignsDepartmentHead() {
        User staff = user(2L, Role.STAFF);
        User head = user(8L, Role.DEPT_HEAD);
        head.setDepartment("IT Services");
        Category category = new Category();
        category.setDepartment("IT Services");
        category.setSlaHours(24);
        Ticket open = ticket(11L, TicketStatus.OPEN, staff);
        open.setPriority(TicketPriority.MEDIUM);
        open.setCategory(category);
        when(ticketRepository.findById(11L)).thenReturn(Optional.of(open));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authService.staffMembers()).thenReturn(List.of(staff, head));

        Ticket escalated = ticketService.escalate(11L, staff);

        assertThat(escalated.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(escalated.getAssignee()).isEqualTo(head);
        assertThat(escalated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void addAttachmentRejectsFileExceedingSizeLimit() {
        User student = user(1L, Role.STUDENT);
        Ticket ticket = ticket(5L, TicketStatus.OPEN, student);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));

        org.springframework.web.multipart.MultipartFile oversizedFile =
                org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(oversizedFile.isEmpty()).thenReturn(false);
        when(oversizedFile.getSize()).thenReturn(6L * 1024L * 1024L);

        assertThatThrownBy(() -> ticketService.addAttachment(5L, student, oversizedFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File exceeds the 5 MB limit.");
    }

    @Test
    void removeAttachmentDeletesFileOnlyAfterCommit() throws Exception {
        User student = user(1L, Role.STUDENT);
        Ticket ticket = ticket(9L, TicketStatus.OPEN, student);
        Path file = ticketService.resolveStoredFile("/uploads/9/note.png");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "png");
        TicketAttachment attachment = new TicketAttachment();
        attachment.setId(4L);
        attachment.setTicket(ticket);
        attachment.setFileName("note.png");
        attachment.setFilePath("/uploads/9/note.png");
        when(ticketRepository.findById(9L)).thenReturn(Optional.of(ticket));
        when(attachmentRepository.findById(4L)).thenReturn(Optional.of(attachment));

        TransactionSynchronizationManager.initSynchronization();
        try {
            ticketService.removeAttachment(9L, 4L, student);
            assertThat(Files.exists(file)).isTrue();
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(attachmentRepository).delete(attachment);
        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    void resolveStoredFileRejectsPathTraversal() {
        assertThatThrownBy(() -> ticketService.resolveStoredFile("/uploads/../secret.txt"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ownerConfirmsResolvedTicketAndCanSendItBackToTheSameAssignee() {
        User student = user(1L, Role.STUDENT);
        User assignee = user(2L, Role.STAFF);
        Ticket resolved = ticket(11L, TicketStatus.RESOLVED, student);
        resolved.setAssignee(assignee);
        resolved.setResolvedAt(LocalDateTime.now().minusDays(1));
        when(ticketRepository.findById(11L)).thenReturn(Optional.of(resolved));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket closed = ticketService.confirmClose(11L, student);
        assertThat(closed.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(closed.getAssignee()).isSameAs(assignee);
        assertThat(closed.getResolvedAt()).isNotNull();
        verify(auditService).log(eq(student), contains("requester confirmed"), eq("TICKET"), eq(11L));

        resolved.setStatus(TicketStatus.RESOLVED);
        Ticket sentBack = ticketService.reopenResolved(11L, student);
        assertThat(sentBack.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(sentBack.getAssignee()).isSameAs(assignee);
        assertThat(sentBack.getResolvedAt()).isNull();
    }

    @Test
    void staffCloseResolvedRequiresAReasonAndAutoCloseLogsHow() {
        User staff = user(2L, Role.STAFF);
        User student = user(1L, Role.STUDENT);
        Ticket resolved = ticket(12L, TicketStatus.RESOLVED, student);
        resolved.setAssignee(staff);
        resolved.setResolvedAt(LocalDateTime.now().minusDays(1));
        when(ticketRepository.findById(12L)).thenReturn(Optional.of(resolved));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> ticketService.updateStatus(12L, staff, TicketStatus.CLOSED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
        assertThatThrownBy(() -> ticketService.staffCloseResolved(12L, staff, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");

        Ticket closed = ticketService.staffCloseResolved(12L, staff, "Duplicate of HD-4");
        assertThat(closed.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(closed.getAssignee()).isSameAs(staff);
        verify(auditService).log(eq(staff), contains("staff override"), eq("TICKET"), eq(12L));

        Ticket stale = ticket(13L, TicketStatus.RESOLVED, student);
        User kept = user(5L, Role.STAFF);
        stale.setAssignee(kept);
        stale.setResolvedAt(LocalDateTime.now().minusDays(6));
        when(ticketRepository.findByStatusAndResolvedAtLessThanEqual(eq(TicketStatus.RESOLVED), any(LocalDateTime.class)))
                .thenReturn(List.of(stale));

        assertThat(ticketService.autoCloseResolvedTickets()).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(stale.getAssignee()).isSameAs(kept);
        verify(auditService).log((User) null, "Closed ticket. How: auto-closed", "TICKET", 13L);
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setEmail("user@sliit.lk");
        user.setFullName("Test User");
        return user;
    }

    private Ticket ticket(Long id, TicketStatus status, User requester) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle("Campus Wi-Fi");
        ticket.setDescription("No signal in the library");
        ticket.setStatus(status);
        ticket.setRequester(requester);
        return ticket;
    }
}
