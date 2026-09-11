package com.sliit.helpdesk.ticket.dto;

import com.sliit.helpdesk.auth.model.User;

public class StaffMemberResponse {

    private Long id;
    private String fullName;
    private String role;

    public static StaffMemberResponse from(User user) {
        StaffMemberResponse response = new StaffMemberResponse();
        response.id = user.getId();
        response.fullName = user.getFullName();
        response.role = user.getRole().name();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }
}
