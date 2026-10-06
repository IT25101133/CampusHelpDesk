package com.sliit.helpdesk.ticket.model;

import java.util.List;

/**
 * Canonical ticket categories. The same spellings are used by validation,
 * the category seeder, and both create and edit forms.
 */
public final class TicketCategories {

    public static final List<String> NAMES = List.of(
            "IT Support",
            "Academic",
            "Library Services",
            "Finance Office",
            "Hostel",
            "Facilities",
            "Examinations",
            "General"
    );

    private TicketCategories() {
    }

    public static boolean isCanonical(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String clean = name.trim();
        for (String canonical : NAMES) {
            if (canonical.equalsIgnoreCase(clean)) {
                return true;
            }
        }
        return false;
    }
}
