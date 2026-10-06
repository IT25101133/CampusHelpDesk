package com.sliit.helpdesk.ticket.controller;

// Ticket Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.ticket.dto.TicketForm;
import com.sliit.helpdesk.ticket.dto.TicketUpdateRequest;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class TicketController {

    private final TicketService ticketService;
    private final AuthService authService;

    public TicketController(TicketService ticketService, AuthService authService) {
        this.ticketService = ticketService;
        this.authService = authService;
    }

    /** READ: ticket list page for the signed-in user. */
    @GetMapping("/tickets")
    public String list(Authentication authentication, Model model) {
        User user = authService.requireByEmail(authentication.getName());
        model.addAttribute("tickets", ticketService.listFor(user));
        model.addAttribute("studentView", ticketService.isSubmitter(user));
        return "ticket/list";
    }

    @GetMapping("/tickets/new")
    public String form(Model model) {
        if (!model.containsAttribute("ticketForm")) {
            model.addAttribute("ticketForm", new TicketForm());
        }
        model.addAttribute("categories", ticketService.selectableCategories());
        model.addAttribute("priorities", TicketPriority.values());
        return "ticket/form";
    }

    /** CREATE: submit the new-ticket form. */
    @PostMapping("/tickets")
    public String create(
            Authentication authentication,
            @Valid @ModelAttribute("ticketForm") TicketForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", ticketService.selectableCategories());
            model.addAttribute("priorities", TicketPriority.values());
            return "ticket/form";
        }
        User user = authService.requireByEmail(authentication.getName());
        Ticket ticket = ticketService.create(user, form);
        redirectAttributes.addFlashAttribute("success", "Ticket " + ticket.getTicketNumber() + " submitted.");
        return "redirect:/tickets/" + ticket.getId();
    }

    /** READ: ticket detail page. */
    @GetMapping("/tickets/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        User user = authService.requireByEmail(authentication.getName());
        Ticket ticket = ticketService.requireVisible(id, user);
        boolean staff = ticketService.isHelpDeskStaff(user);
        boolean owner = ticket.getRequester() != null && ticket.getRequester().getId().equals(user.getId());
        boolean openPhase = ticketService.isOpenPhase(ticket);
        model.addAttribute("ticket", ticket);
        model.addAttribute("comments", ticketService.comments(ticket));
        List<TicketStatus> statuses = new ArrayList<>(Arrays.asList(TicketStatus.values()));
        if (ticket.getAssignee() == null) {
            statuses.remove(TicketStatus.IN_PROGRESS);
        }
        model.addAttribute("statuses", statuses);
        model.addAttribute("staffMembers", authService.staffMembers().stream()
                .filter(ticketService::isHelpDeskStaff)
                .toList());
        model.addAttribute("categories", ticketService.selectableCategories());
        model.addAttribute("canManage", staff);
        model.addAttribute("canEdit", ticketService.canEditTicket(ticket, user));
        model.addAttribute("canDelete", ticketService.canDeleteTicket(ticket, user));
        model.addAttribute("canReopen", ticketService.canReopenTicket(ticket, user));
        model.addAttribute("canComment", ticketService.canCommentOn(ticket, user));
        model.addAttribute("canClose", !staff && owner && openPhase);
        model.addAttribute("closedNotice", ticketService.closedNotice(ticket, user));
        model.addAttribute("canRequestDelete", ticketService.canRequestDelete(ticket, user));
        model.addAttribute("canApproveDelete", ticketService.canApproveDelete(ticket, user));
        model.addAttribute("canConfirmClose", ticketService.canConfirmClose(ticket, user));
        model.addAttribute("canReopenResolved", ticketService.canReopenResolved(ticket, user));
        model.addAttribute("canStaffClose", ticketService.canStaffClose(ticket, user));
        model.addAttribute("openPhase", openPhase);
        return "ticket/detail";
    }

    @PostMapping("/tickets/{id}/comments")
    public String comment(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam String body,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            ticketService.addComment(id, user, body);
            redirectAttributes.addFlashAttribute("success", "Reply posted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/status")
    public String status(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam TicketStatus status,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            ticketService.updateStatus(id, user, status);
            redirectAttributes.addFlashAttribute("success", "Status updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/assign")
    public String assign(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam Long assigneeId,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            ticketService.reassign(id, user, assigneeId);
            redirectAttributes.addFlashAttribute("success", "Ticket reassigned.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/category")
    public String changeCategory(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam Long categoryId,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            TicketUpdateRequest request = new TicketUpdateRequest();
            request.setCategoryId(categoryId);
            ticketService.update(id, user, request);
            redirectAttributes.addFlashAttribute("success", "Department / category updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/reopen")
    public String reopen(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            ticketService.reopen(id, user, reason);
            redirectAttributes.addFlashAttribute("success", "Ticket reopened.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    /** DELETE: remove the ticket when the current user is allowed to. */
    @PostMapping("/tickets/{id}/withdraw")
    public String withdraw(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            ticketService.delete(id, user);
            redirectAttributes.addFlashAttribute("success", "Ticket withdrawn.");
            return "redirect:/tickets";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/tickets/" + id;
        }
    }

    @PostMapping("/tickets/{id}/confirm-close")
    public String confirmClose(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            ticketService.confirmClose(id, user);
            redirectAttributes.addFlashAttribute("success", "Ticket closed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/reopen-resolved")
    public String reopenResolved(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            ticketService.reopenResolved(id, user);
            redirectAttributes.addFlashAttribute("success", "Ticket sent back to the assignee.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/staff-close")
    public String staffClose(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            ticketService.staffCloseResolved(id, user, reason);
            redirectAttributes.addFlashAttribute("success", "Ticket closed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/close")
    public String close(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            ticketService.updateStatus(id, user, TicketStatus.CLOSED);
            redirectAttributes.addFlashAttribute("success", "Ticket closed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/delete-request")
    public String requestDelete(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            ticketService.requestDelete(id, user);
            redirectAttributes.addFlashAttribute("success", "Deletion requested. Help-desk staff must approve it.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/delete-approval")
    public String approveDelete(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            ticketService.approveDelete(id, user);
            redirectAttributes.addFlashAttribute("success", "Deletion approved.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    /** UPDATE: save title, description, or category from the ticket page. */
    @PostMapping("/tickets/{id}")
    public String update(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long categoryId,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            TicketUpdateRequest request = new TicketUpdateRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setPriority(priority);
            request.setCategoryId(categoryId);
            ticketService.update(id, user, request);
            redirectAttributes.addFlashAttribute("success", "Ticket updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }
}
