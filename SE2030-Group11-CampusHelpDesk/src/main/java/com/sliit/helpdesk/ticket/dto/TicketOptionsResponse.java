package com.sliit.helpdesk.ticket.dto;

import com.sliit.helpdesk.ticket.model.TicketPriority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TicketOptionsResponse {

    private List<CategoryResponse> categories = new ArrayList<>();
    private List<TicketPriority> priorities = Arrays.asList(TicketPriority.values());
    private List<StaffMemberResponse> staffMembers = new ArrayList<>();

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
}
