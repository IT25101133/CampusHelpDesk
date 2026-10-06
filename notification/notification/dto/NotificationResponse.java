package com.sliit.helpdesk.notification.dto;

import com.sliit.helpdesk.notification.model.Notification;

import java.time.LocalDateTime;

public class NotificationResponse {

    private Long id;
    private String message;
    private boolean read;
    private Long ticketId;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.id = notification.getId();
        response.message = notification.getMessage();
        response.read = notification.isRead();
        response.ticketId = notification.getTicket() == null ? null : notification.getTicket().getId();
        response.createdAt = notification.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
