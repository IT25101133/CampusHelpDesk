package com.sliit.helpdesk.ticket.service;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.ticket.dto.TicketForm;
import com.sliit.helpdesk.ticket.model.Ticket;

/**
 * Concrete Factory Method creator for students and lecturers.
 */
public final class StudentTicketCreator extends TicketCreator {

    @Override
    protected Ticket createTicket(User requester, TicketForm form, Category category) {
        return openTicket(requester, form, category);
    }

    @Override
    protected Role fallbackRole() {
        return Role.STUDENT;
    }
}
