package com.sliit.helpdesk.ticket.service;

/**
 * Global constant definitions for ticket attachment handling.
 */
public final class TicketAttachmentConstants {

    /** Default maximum ticket attachment limit in megabytes. */
    public static final int MAX_TICKET_ATTACHMENT_MB = 5;

    /** Default maximum ticket attachment limit in bytes. */
    public static final long MAX_TICKET_ATTACHMENT_BYTES = MAX_TICKET_ATTACHMENT_MB * 1024L * 1024L;

    private TicketAttachmentConstants() {
        // Prevent instantiation
    }
}
