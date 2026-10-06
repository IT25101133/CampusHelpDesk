package com.sliit.helpdesk.ticket.service;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.category.service.AssignmentService;
import com.sliit.helpdesk.category.service.CategoryService;
import com.sliit.helpdesk.notification.model.TicketComment;
import com.sliit.helpdesk.notification.service.NotificationService;
import com.sliit.helpdesk.report.service.AuditService;
import com.sliit.helpdesk.ticket.dto.TicketForm;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketCommentRepository;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final CategoryService categoryService;
    private final AssignmentService assignmentService;
    private final NotificationService notificationService;
    private final AuthService authService;
    private final AuditService auditService;

    public TicketService(
            TicketRepository ticketRepository,
            TicketCommentRepository commentRepository,
            CategoryService categoryService,
            AssignmentService assignmentService,
            NotificationService notificationService,
            AuthService authService,
            AuditService auditService
    ) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.categoryService = categoryService;
        this.assignmentService = assignmentService;
        this.notificationService = notificationService;
        this.authService = authService;
        this.auditService = auditService;
    }

    public List<Ticket> listFor(User user) {
        if (user.getRole() == Role.STUDENT) {
            return ticketRepository.findByRequesterOrderByCreatedAtDesc(user);
        }
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    public Ticket requireVisible(Long id, User user) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        if (user.getRole() == Role.STUDENT && !ticket.getRequester().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot view this ticket.");
        }
        return ticket;
    }

    public List<TicketComment> comments(Ticket ticket) {
        return commentRepository.findByTicketOrderByCreatedAtAsc(ticket);
    }

    @Transactional
    public Ticket create(User requester, TicketForm form) {
        Category category = categoryService.requireActive(form.getCategoryId());
        Ticket ticket = new Ticket();
        ticket.setTitle(form.getTitle().trim());
        ticket.setDescription(form.getDescription().trim());
        ticket.setPriority(form.getPriority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCategory(category);
        ticket.setRequester(requester);
        assignmentService.assignDefaultStaff(ticket, category);
        Ticket saved = ticketRepository.save(ticket);
        auditService.record(requester, "Created ticket", "TICKET", saved.getId());

        if (saved.getAssignee() != null) {
            notificationService.notify(
                    saved.getAssignee(),
                    "New ticket assigned: " + saved.getTicketNumber() + " — " + saved.getTitle(),
                    saved
            );
        }
        return saved;
    }

    @Transactional
    public void addComment(Long ticketId, User author, String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Comment cannot be empty.");
        }
        Ticket ticket = requireVisible(ticketId, author);
        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setBody(body.trim());
        commentRepository.save(comment);
        auditService.record(author, "Commented on ticket", "TICKET", ticket.getId());

        User target = author.getId().equals(ticket.getRequester().getId())
                ? ticket.getAssignee()
                : ticket.getRequester();
        if (target != null && !target.getId().equals(author.getId())) {
            notificationService.notify(
                    target,
                    "New reply on " + ticket.getTicketNumber() + " from " + author.getFullName(),
                    ticket
            );
        }
    }

    @Transactional
    public void updateStatus(Long ticketId, User actor, TicketStatus status) {
        if (actor.getRole() == Role.STUDENT) {
            throw new IllegalArgumentException("Only staff can change ticket status.");
        }
        Ticket ticket = requireVisible(ticketId, actor);
        ticket.setStatus(status);
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else {
            ticket.setResolvedAt(null);
        }
        ticketRepository.save(ticket);
        auditService.record(actor, "Updated ticket status to " + status.name(), "TICKET", ticket.getId());
        notificationService.notify(
                ticket.getRequester(),
                "Ticket " + ticket.getTicketNumber() + " is now " + status.name().replace('_', ' '),
                ticket
        );
    }

    @Transactional
    public void reassign(Long ticketId, User actor, Long assigneeId) {
        if (actor.getRole() == Role.STUDENT) {
            throw new IllegalArgumentException("Only staff can reassign tickets.");
        }
        Ticket ticket = requireVisible(ticketId, actor);
        User assignee = authService.staffMembers().stream()
                .filter(u -> u.getId().equals(assigneeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Assignee must be staff."));
        ticket.setAssignee(assignee);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticketRepository.save(ticket);
        auditService.record(actor, "Assigned ticket to " + assignee.getFullName(), "TICKET", ticket.getId());
        notificationService.notify(
                assignee,
                "Ticket assigned to you: " + ticket.getTicketNumber() + " — " + ticket.getTitle(),
                ticket
        );
    }
}
