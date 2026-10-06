package com.sliit.helpdesk.ticket.controller;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.category.service.CategoryService;
import com.sliit.helpdesk.ticket.dto.TicketForm;
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

@Controller
public class TicketController {

    private final TicketService ticketService;
    private final CategoryService categoryService;
    private final AuthService authService;

    public TicketController(TicketService ticketService, CategoryService categoryService, AuthService authService) {
        this.ticketService = ticketService;
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @GetMapping("/tickets")
    public String list(Authentication authentication, Model model) {
        User user = authService.requireByEmail(authentication.getName());
        model.addAttribute("tickets", ticketService.listFor(user));
        model.addAttribute("studentView", user.getRole() == Role.STUDENT);
        return "ticket/list";
    }

    @GetMapping("/tickets/new")
    public String form(Model model) {
        if (!model.containsAttribute("ticketForm")) {
            model.addAttribute("ticketForm", new TicketForm());
        }
        model.addAttribute("categories", categoryService.activeCategories());
        model.addAttribute("priorities", TicketPriority.values());
        return "ticket/form";
    }

    @PostMapping("/tickets")
    public String create(
            Authentication authentication,
            @Valid @ModelAttribute("ticketForm") TicketForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.activeCategories());
            model.addAttribute("priorities", TicketPriority.values());
            return "ticket/form";
        }
        User user = authService.requireByEmail(authentication.getName());
        Ticket ticket = ticketService.create(user, form);
        redirectAttributes.addFlashAttribute("success", "Ticket " + ticket.getTicketNumber() + " submitted.");
        return "redirect:/tickets/" + ticket.getId();
    }

    @GetMapping("/tickets/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        User user = authService.requireByEmail(authentication.getName());
        Ticket ticket = ticketService.requireVisible(id, user);
        model.addAttribute("ticket", ticket);
        model.addAttribute("comments", ticketService.comments(ticket));
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("staffMembers", authService.staffMembers());
        model.addAttribute("canManage", user.getRole() != Role.STUDENT);
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
}
