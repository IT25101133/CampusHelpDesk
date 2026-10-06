package com.sliit.helpdesk.ticket.dto;

// Category Response is part of the campus help desk dto code.



import com.sliit.helpdesk.category.model.Category;



/** One department choice for a ticket. */
public class CategoryResponse {



    /** Database id of the category. */
    private Long id;

    /** Name shown in the dropdown, such as IT Support. */
    private String name;

    /** Faculty or office this category belongs to. */
    private String department;

    /** Hours allowed before the ticket is overdue. */
    private int slaHours;



    public static CategoryResponse from(Category category) {

        CategoryResponse response = new CategoryResponse();

        response.id = category.getId();

        response.name = category.getName();

        response.department = category.getDepartment();

        response.slaHours = category.getSlaHours();

        return response;

    }



    public Long getId() {

        return id;

    }



    public String getName() {

        return name;

    }



    public String getDepartment() {

        return department;

    }



    public int getSlaHours() {

        return slaHours;

    }

}

