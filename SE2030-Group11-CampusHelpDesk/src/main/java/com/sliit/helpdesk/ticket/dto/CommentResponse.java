package com.sliit.helpdesk.ticket.dto;

import com.sliit.helpdesk.notification.model.TicketComment;

import java.time.LocalDateTime;

public class CommentResponse {

    private Long id;
    private String author;
    private String body;
    private LocalDateTime createdAt;

    public static CommentResponse from(TicketComment comment) {
        CommentResponse response = new CommentResponse();
        response.id = comment.getId();
        response.author = comment.getAuthor() == null ? "" : comment.getAuthor().getFullName();
        response.body = comment.getBody();
        response.createdAt = comment.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
