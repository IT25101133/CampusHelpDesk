package com.sliit.helpdesk.ticket.service;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.ticket.dto.TicketForm;
import com.sliit.helpdesk.ticket.model.Ticket;
import org.springframework.stereotype.Component;

/**
 * Chooses the Factory Method creator for a new ticket and reapplies the
 * shared {@link SlaPolicy} when priority or category changes later.
 */
@Component
public class TicketFactory {

    private final TicketCreator studentTickets = new StudentTicketCreator();
    private final TicketCreator staffTickets = new StaffTicketCreator();

    public Ticket create(User requester, TicketForm form, Category category) {
        return creatorFor(requester).create(requester, form, category);
    }

    public void applySla(Ticket ticket, Category category) {
        SlaPolicy.getInstance().apply(ticket, category);
    }

    private TicketCreator creatorFor(User requester) {
        Role role = requester == null ? null : requester.getRole();
        if (role == Role.STAFF || role == Role.ADMIN || role == Role.DEPT_HEAD) {
            return staffTickets;
        }
        return studentTickets;
    }
}
