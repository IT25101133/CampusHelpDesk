package com.sliit.helpdesk.ticket.dto;

// Ticket Options Response is part of the campus help desk dto code.

import com.sliit.helpdesk.ticket.model.TicketPriority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Dropdown values for the new-ticket and ticket-detail screens. */
public class TicketOptionsResponse {

    /** Departments the user can pick. */
    private List<CategoryResponse> categories = new ArrayList<>();
    /** Low, medium, high, and critical. */
    private List<TicketPriority> priorities = Arrays.asList(TicketPriority.values());
    /** Staff who can be assigned. Empty for students and lecturers. */
    private List<StaffMemberResponse> staffMembers = new ArrayList<>();
    /** Largest file a user may attach, in megabytes. */
    private int maxAttachmentMb = 5;

    public List<CategoryResponse> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryResponse> categories) {
        this.categories = categories;
    }

    public List<TicketPriority> getPriorities() {
        return priorities;
    }

    public void setPriorities(List<TicketPriority> priorities) {
        this.priorities = priorities;
    }

    public List<StaffMemberResponse> getStaffMembers() {
        return staffMembers;
    }

    public void setStaffMembers(List<StaffMemberResponse> staffMembers) {
        this.staffMembers = staffMembers;
    }

    public int getMaxAttachmentMb() {
        return maxAttachmentMb;
    }

    public void setMaxAttachmentMb(int maxAttachmentMb) {
        this.maxAttachmentMb = maxAttachmentMb;
    }
}
