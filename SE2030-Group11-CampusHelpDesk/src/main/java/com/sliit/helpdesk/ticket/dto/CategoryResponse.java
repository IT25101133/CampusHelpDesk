package com.sliit.helpdesk.ticket.dto;

import com.sliit.helpdesk.category.model.Category;

public class CategoryResponse {

    private Long id;
    private String name;
    private String department;
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
