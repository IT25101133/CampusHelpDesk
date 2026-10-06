package com.sliit.helpdesk.ticket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Existing tickets predate {@code submitter_role}. Copy the requester's current
 * role onto those rows. New tickets store the role at submit time instead.
 */
@Component
@Order(20)
public class TicketSubmitterRoleBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketSubmitterRoleBackfill.class);

    private final TicketService ticketService;

    public TicketSubmitterRoleBackfill(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int updated = ticketService.backfillSubmitterRoles();
        if (updated > 0) {
            log.info(
                    "Backfilled submitter_role on {} existing ticket(s) from each requester's current role",
                    updated
            );
        }
    }
}
