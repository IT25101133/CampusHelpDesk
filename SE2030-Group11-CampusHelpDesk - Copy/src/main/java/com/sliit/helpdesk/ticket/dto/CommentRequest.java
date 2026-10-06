package com.sliit.helpdesk.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public class CommentRequest {

    @NotBlank
    private String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
