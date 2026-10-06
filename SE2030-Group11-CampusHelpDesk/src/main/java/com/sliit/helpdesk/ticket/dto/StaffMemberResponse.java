package com.sliit.helpdesk.ticket.dto;

// Staff Member Response is part of the campus help desk dto code.



import com.sliit.helpdesk.auth.model.User;



/** One person who can be assigned to a ticket. */
public class StaffMemberResponse {



    /** User id used when assigning the ticket. */
    private Long id;

    /** Name shown in the assign dropdown. */
    private String fullName;

    /** Staff, admin, or department head. */
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

