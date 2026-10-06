package com.sliit.helpdesk.ticket.controller;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.category.service.CategoryService;
import com.sliit.helpdesk.ticket.dto.AssignRequest;
import com.sliit.helpdesk.ticket.dto.CategoryResponse;
import com.sliit.helpdesk.ticket.dto.CommentRequest;
import com.sliit.helpdesk.ticket.dto.CommentResponse;
import com.sliit.helpdesk.ticket.dto.StaffMemberResponse;
import com.sliit.helpdesk.ticket.dto.StatusUpdateRequest;
import com.sliit.helpdesk.ticket.dto.TicketForm;
import com.sliit.helpdesk.ticket.dto.TicketOptionsResponse;
import com.sliit.helpdesk.ticket.dto.TicketResponse;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketApiController {

    private final TicketService ticketService;
    private final CategoryService categoryService;
    private final AuthService authService;

    public TicketApiController(TicketService ticketService, CategoryService categoryService, AuthService authService) {
        this.ticketService = ticketService;
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<TicketResponse> list(Authentication authentication) {
        User user = currentUser(authentication);
        return ticketService.listFor(user).stream().map(TicketResponse::from).toList();
    }

    @GetMapping("/options")
    @Transactional(readOnly = true)
    public TicketOptionsResponse options(Authentication authentication) {
        User user = currentUser(authentication);
        TicketOptionsResponse options = new TicketOptionsResponse();
        options.setCategories(categoryService.activeCategories().stream().map(CategoryResponse::from).toList());
        if (user.getRole() != Role.STUDENT) {
            options.setStaffMembers(authService.staffMembers().stream().map(StaffMemberResponse::from).toList());
        }
        return options;
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public TicketResponse detail(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        Ticket ticket = ticketService.requireVisible(id, user);
        TicketResponse response = TicketResponse.from(ticket);
        response.setComments(ticketService.comments(ticket).stream().map(CommentResponse::from).toList());
        response.setCanManage(user.getRole() != Role.STUDENT);
        if (response.isCanManage()) {
            response.setStaffMembers(authService.staffMembers().stream().map(StaffMemberResponse::from).toList());
        }
        return response;
    }

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

    @PostMapping("/{id}/comments")
    @Transactional
    public ResponseEntity<Map<String, String>> comment(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody CommentRequest request
    ) {
        ticketService.addComment(id, currentUser(authentication), request.getBody());
        return ResponseEntity.ok(Map.of("status", "ok"));
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

    private User currentUser(Authentication authentication) {
        return authService.requireByEmail(authentication.getName());
    }
}
