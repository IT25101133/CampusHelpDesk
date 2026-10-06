package com.sliit.helpdesk.ticket;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.category.service.AssignmentService;
import com.sliit.helpdesk.category.service.CategoryService;
import com.sliit.helpdesk.notification.service.NotificationService;
import com.sliit.helpdesk.report.service.AuditService;
import com.sliit.helpdesk.ticket.dto.TicketForm;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketCommentRepository;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import com.sliit.helpdesk.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TicketCommentRepository commentRepository;
    @Mock private CategoryService categoryService;
    @Mock private AssignmentService assignmentService;
    @Mock private NotificationService notificationService;
    @Mock private AuthService authService;
    @Mock private AuditService auditService;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository, commentRepository, categoryService,
                assignmentService, notificationService, authService, auditService
        );
    }

    @Test
    void createOpensTicketForRequester() {
        User student = user(1L, Role.STUDENT);
        Category category = new Category();
        category.setName("IT Support");
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

        assertThat(ticket.getTicketNumber()).isEqualTo("HD-4");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getRequester()).isEqualTo(student);
        assertThat(ticket.getTitle()).isEqualTo("LMS down");
    }

    @Test
    void studentCannotChangeStatus() {
        User student = user(1L, Role.STUDENT);
        assertThatThrownBy(() -> ticketService.updateStatus(1L, student, TicketStatus.CLOSED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only staff");
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setEmail("user@sliit.lk");
        user.setFullName("Test User");
        return user;
    }
}
