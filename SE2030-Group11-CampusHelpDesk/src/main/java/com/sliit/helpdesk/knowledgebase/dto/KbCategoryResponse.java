package com.sliit.helpdesk.knowledgebase.dto;

// Kb Category Response is part of the campus help desk dto code.

import com.sliit.helpdesk.knowledgebase.model.KbCategory;

public class KbCategoryResponse {

    private Long id;
    private String name;

    public static KbCategoryResponse from(KbCategory category) {
        KbCategoryResponse response = new KbCategoryResponse();
        response.id = category.getId();
        response.name = category.getName();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
