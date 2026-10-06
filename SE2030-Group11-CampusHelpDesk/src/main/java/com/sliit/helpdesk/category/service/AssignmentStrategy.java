package com.sliit.helpdesk.category.service;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketStatus;

/**
 * Strategy for choosing a staff assignee when a ticket is created or routed.
 */
public interface AssignmentStrategy {

    void assign(Ticket ticket);

    static void apply(Ticket ticket, User assignee) {
        ticket.setAssignee(assignee);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
    }
}
