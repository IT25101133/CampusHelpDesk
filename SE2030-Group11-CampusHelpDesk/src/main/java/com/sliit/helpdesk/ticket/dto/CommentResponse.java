package com.sliit.helpdesk.ticket.dto;

// Comment Response is part of the campus help desk dto code.

import com.sliit.helpdesk.notification.model.TicketComment;

import java.time.LocalDateTime;

/** One reply returned to the page. */
public class CommentResponse {

    /** Database id of the reply. */
    private Long id;
    /** Name of the person who wrote it. */
    private String author;
    /** Id of that person. */
    private Long authorId;
    /** The reply text. */
    private String body;
    /** When the reply was posted. */
    private LocalDateTime createdAt;

    public static CommentResponse from(TicketComment comment) {
        CommentResponse response = new CommentResponse();
        response.id = comment.getId();
        response.author = comment.getAuthor() == null ? "" : comment.getAuthor().getFullName();
        response.authorId = comment.getAuthor() == null ? null : comment.getAuthor().getId();
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

    public Long getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
