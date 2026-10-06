package com.sliit.helpdesk.ticket.service;

import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;

import java.time.LocalDateTime;

/**
 * Singleton SLA policy. Every ticket factory shares this one instance when it
 * turns a category's {@code sla_hours} into a due time.
 */
public final class SlaPolicy {

    private static final SlaPolicy INSTANCE = new SlaPolicy();

    private SlaPolicy() {
    }

    public static SlaPolicy getInstance() {
        return INSTANCE;
    }

    /**
     * Scales the category window by priority: CRITICAL 25%, HIGH 50%,
     * MEDIUM 100%, LOW 200% of {@code ticket_categories.sla_hours}.
     */
    public void apply(Ticket ticket, Category category) {
        if (ticket == null || category == null) {
            return;
        }
        int baseHours = category.getSlaHours() > 0 ? category.getSlaHours() : 48;
        ticket.setSlaDueAt(LocalDateTime.now().plusHours(resolveHours(baseHours, ticket.getPriority())));
    }

    public int resolveHours(int baseHours, TicketPriority priority) {
        int hours = baseHours > 0 ? baseHours : 48;
        TicketPriority resolved = priority == null ? TicketPriority.MEDIUM : priority;
        return switch (resolved) {
            case CRITICAL -> Math.max(1, hours / 4);
            case HIGH -> Math.max(1, hours / 2);
            case MEDIUM -> hours;
            case LOW -> hours * 2;
        };
    }
}
