package com.sliit.helpdesk.ticket.controller;

// Ticket Api Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.report.dto.AuditLogResponse;
import com.sliit.helpdesk.ticket.dto.AssignRequest;
import com.sliit.helpdesk.ticket.dto.CloseRequest;
import com.sliit.helpdesk.ticket.dto.AttachmentResponse;
import com.sliit.helpdesk.ticket.dto.CategoryResponse;
import com.sliit.helpdesk.ticket.dto.CommentResponse;
import com.sliit.helpdesk.ticket.dto.StaffMemberResponse;
import com.sliit.helpdesk.ticket.dto.ReopenRequest;
import com.sliit.helpdesk.ticket.dto.StatusUpdateRequest;
import com.sliit.helpdesk.ticket.dto.TicketFeedbackRequest;
import com.sliit.helpdesk.ticket.dto.TicketForm;
import com.sliit.helpdesk.ticket.dto.TicketOptionsResponse;
import com.sliit.helpdesk.ticket.dto.TicketResponse;
import com.sliit.helpdesk.ticket.dto.TicketUpdateRequest;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketApiController {

    private final TicketService ticketService;
    private final AuthService authService;

    public TicketApiController(TicketService ticketService, AuthService authService) {
        this.ticketService = ticketService;
        this.authService = authService;
    }

    /** READ: list tickets visible to the signed-in user. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<TicketResponse> list(
            Authentication authentication,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId
    ) {
        User user = currentUser(authentication);
        return ticketService.listFor(user, status, priority, createdBy, q, categoryId).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @GetMapping("/options")
    @Transactional(readOnly = true)
    public TicketOptionsResponse options(Authentication authentication) {
        User user = currentUser(authentication);
        TicketOptionsResponse options = new TicketOptionsResponse();
        options.setCategories(ticketService.selectableCategories().stream().map(CategoryResponse::from).toList());
        options.setStaffMembers(staffFor(user));
        options.setMaxAttachmentMb(ticketService.getMaxAttachmentMb());
        return options;
    }

    /** READ: one ticket, including comments, files, and what this user may do. */
    @GetMapping("/{id:\\d+}")
    @Transactional(readOnly = true)
    public TicketResponse detail(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        Ticket ticket = ticketService.requireVisible(id, user);
        TicketResponse response = TicketResponse.from(ticket);
        response.setComments(ticketService.comments(ticket).stream().map(CommentResponse::from).toList());
        response.setAttachments(ticketService.attachments(ticket).stream().map(AttachmentResponse::from).toList());
        applyPermissions(response, ticket, user);
        if (response.isCanManage()) {
            response.setStaffMembers(staffFor(user));
        }
        return response;
    }

    /** CREATE: submit a new ticket. */
    @PostMapping
    @Transactional
    public ResponseEntity<TicketResponse> create(
            Authentication authentication,
            @Valid @RequestBody TicketForm form
    ) {
        User user = currentUser(authentication);
        Ticket ticket = ticketService.create(user, form);
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponse.from(ticket));
    }

    /** UPDATE: change the ticket title, description, or category. */
    @PutMapping("/{id}")
    @Transactional
    public TicketResponse update(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody TicketUpdateRequest request
    ) {
        Ticket ticket = ticketService.update(id, currentUser(authentication), request);
        return TicketResponse.from(ticket);
    }

    /** DELETE: withdraw an open unassigned ticket, or hard-delete when the caller is an administrator. */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> withdraw(@PathVariable Long id, Authentication authentication) {
        ticketService.delete(id, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/delete-request")
    @Transactional
    public TicketResponse requestDelete(@PathVariable Long id, Authentication authentication) {
        Ticket ticket = ticketService.requestDelete(id, currentUser(authentication));
        return TicketResponse.from(ticket);
    }

    @PostMapping("/{id}/delete-approval")
    @Transactional
    public TicketResponse approveDelete(@PathVariable Long id, Authentication authentication) {
        Ticket ticket = ticketService.approveDelete(id, currentUser(authentication));
        return TicketResponse.from(ticket);
    }

    @PostMapping("/{id}/confirm-close")
    @Transactional
    public TicketResponse confirmClose(@PathVariable Long id, Authentication authentication) {
        return TicketResponse.from(ticketService.confirmClose(id, currentUser(authentication)));
    }

    @PostMapping("/{id}/reopen-resolved")
    @Transactional
    public TicketResponse reopenResolved(@PathVariable Long id, Authentication authentication) {
        return TicketResponse.from(ticketService.reopenResolved(id, currentUser(authentication)));
    }

    @PostMapping("/{id}/staff-close")
    @Transactional
    public TicketResponse staffClose(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody CloseRequest request
    ) {
        return TicketResponse.from(ticketService.staffCloseResolved(id, currentUser(authentication), request.getReason()));
    }

    @PutMapping("/{id}/reopen")
    @Transactional
    public TicketResponse reopen(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody ReopenRequest request
    ) {
        Ticket ticket = ticketService.reopen(id, currentUser(authentication), request.getReason());
        return TicketResponse.from(ticket);
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        var attachment = ticketService.addAttachment(id, currentUser(authentication), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(AttachmentResponse.from(attachment));
    }

    @GetMapping("/{id:\\d+}/attachments")
    @Transactional(readOnly = true)
    public List<AttachmentResponse> attachments(@PathVariable Long id, Authentication authentication) {
        return ticketService.attachments(ticketService.requireVisible(id, currentUser(authentication))).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @GetMapping("/{id:\\d+}/attachments/{attachmentId:\\d+}")
    @Transactional(readOnly = true)
    public AttachmentResponse attachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId,
            Authentication authentication
    ) {
        return AttachmentResponse.from(ticketService.requireAttachment(id, attachmentId, currentUser(authentication)));
    }

    @DeleteMapping("/{id:\\d+}/attachments/{attachmentId:\\d+}")
    @Transactional
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId,
            Authentication authentication
    ) {
        ticketService.removeAttachment(id, attachmentId, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/status")
    @Transactional
    public ResponseEntity<Map<String, String>> status(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        ticketService.updateStatus(id, currentUser(authentication), request.getStatus());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/{id}/assign")
    @Transactional
    public ResponseEntity<Map<String, String>> assign(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody AssignRequest request
    ) {
        ticketService.reassign(id, currentUser(authentication), request.getAssigneeId());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PutMapping("/{id}/reassign")
    @Transactional
    public TicketResponse reassign(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody AssignRequest request
    ) {
        Ticket ticket = ticketService.reassign(id, currentUser(authentication), request.getAssigneeId());
        return TicketResponse.from(ticket);
    }

    @PutMapping("/{id}/escalate")
    @Transactional
    public TicketResponse escalate(@PathVariable Long id, Authentication authentication) {
        Ticket ticket = ticketService.escalate(id, currentUser(authentication));
        return TicketResponse.from(ticket);
    }

    @GetMapping("/{id:\\d+}/history")
    @Transactional(readOnly = true)
    public List<AuditLogResponse> history(@PathVariable Long id, Authentication authentication) {
        return ticketService.history(id, currentUser(authentication)).stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @PostMapping("/{id}/feedback")
    @Transactional
    public ResponseEntity<CommentResponse> feedback(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody TicketFeedbackRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(
                ticketService.addFeedback(id, currentUser(authentication), request.getRating(), request.getComment())
        ));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authentication required. Please sign in again.");
        }
        return authService.requireByEmail(authentication.getName());
    }

    private List<StaffMemberResponse> staffFor(User user) {
        if (!ticketService.isHelpDeskStaff(user)) {
            return List.of();
        }
        return authService.staffMembers().stream()
                .filter(ticketService::isHelpDeskStaff)
                .map(StaffMemberResponse::from)
                .toList();
    }

    private void applyPermissions(TicketResponse response, Ticket ticket, User user) {
        response.setCanManage(ticketService.isHelpDeskStaff(user));
        response.setCanEdit(ticketService.canEditTicket(ticket, user));
        response.setCanDelete(ticketService.canDeleteTicket(ticket, user));
        response.setCanReopen(ticketService.canReopenTicket(ticket, user));
        response.setCanComment(ticketService.canCommentOn(ticket, user));
        response.setClosedNotice(ticketService.closedNotice(ticket, user));
        response.setCanRequestDelete(ticketService.canRequestDelete(ticket, user));
        response.setCanApproveDelete(ticketService.canApproveDelete(ticket, user));
        response.setCanConfirmClose(ticketService.canConfirmClose(ticket, user));
        response.setCanReopenResolved(ticketService.canReopenResolved(ticket, user));
        response.setCanStaffClose(ticketService.canStaffClose(ticket, user));
    }
}
