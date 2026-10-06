package com.sliit.helpdesk.ticket.model;

// Ticket Status is part of the campus help desk model code.

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    @JsonCreator
    public static TicketStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return OPEN;
        }
        String clean = value.trim().toUpperCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        try {
            return TicketStatus.valueOf(clean);
        } catch (IllegalArgumentException ex) {
            return OPEN;
        }
    }
}
