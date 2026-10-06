package com.sliit.helpdesk.ticket.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Closes resolved tickets that nobody confirmed or reopened. */
@Component
public class ResolvedTicketAutoCloser {

    private final TicketService ticketService;

    public ResolvedTicketAutoCloser(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Scheduled(initialDelay = 3_600_000, fixedDelay = 3_600_000)
    public void closeStaleResolvedTickets() {
        ticketService.autoCloseResolvedTickets();
    }
}
