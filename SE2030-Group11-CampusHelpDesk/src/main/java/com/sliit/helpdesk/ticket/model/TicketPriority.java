package com.sliit.helpdesk.ticket.model;

// Ticket Priority is part of the campus help desk model code.

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public TicketPriority escalate() {
        return switch (this) {
            case LOW -> MEDIUM;
            case MEDIUM -> HIGH;
            case HIGH, CRITICAL -> CRITICAL;
        };
    }

    @JsonCreator
    public static TicketPriority fromString(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        String clean = value.trim().toUpperCase(Locale.ROOT);
        if ("MED".equals(clean)) {
            return MEDIUM;
        }
        if ("CRIT".equals(clean)) {
            return CRITICAL;
        }
        try {
            return TicketPriority.valueOf(clean);
        } catch (IllegalArgumentException ex) {
            return MEDIUM;
        }
    }
}
