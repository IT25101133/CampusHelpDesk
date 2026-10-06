package com.sliit.helpdesk.ticket.service;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.ticket.dto.TicketForm;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;

/**
 * Factory Method creator. Subclasses decide which open ticket to build;
 * the shared SLA policy then sets the due time.
 */
public abstract class TicketCreator {

    public final Ticket create(User requester, TicketForm form, Category category) {
        Ticket ticket = createTicket(requester, form, category);
        SlaPolicy.getInstance().apply(ticket, category);
        return ticket;
    }

    /**
     * Factory method. Each concrete creator returns a new open ticket.
     */
    protected abstract Ticket createTicket(User requester, TicketForm form, Category category);

    protected abstract Role fallbackRole();

    protected final Ticket openTicket(User requester, TicketForm form, Category category) {
        Ticket ticket = new Ticket();
        ticket.setTitle(form != null && form.getTitle() != null ? form.getTitle().trim() : "");
        ticket.setDescription(form != null && form.getDescription() != null ? form.getDescription().trim() : "");
        ticket.setPriority(form == null || form.getPriority() == null ? TicketPriority.MEDIUM : form.getPriority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setAssignee(null);
        ticket.setCategory(category);
        ticket.setRequester(requester);
        ticket.setSubmitterRole(requester == null || requester.getRole() == null ? fallbackRole() : requester.getRole());
        ticket.setDeleteRequested(false);
        ticket.setDeleteApproved(false);
        return ticket;
    }
}
