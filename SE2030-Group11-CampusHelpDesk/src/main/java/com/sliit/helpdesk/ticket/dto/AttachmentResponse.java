package com.sliit.helpdesk.ticket.dto;

// Attachment Response is part of the campus help desk dto code.

import com.sliit.helpdesk.ticket.model.TicketAttachment;

import java.time.LocalDateTime;

/** One file attached to a ticket. */
public class AttachmentResponse {

    /** Database id of the file row. */
    private Long id;
    /** Original file name shown to the user. */
    private String fileName;
    /** Where the file can be opened, such as /uploads/12/photo.png. */
    private String filePath;
    /** When the file was uploaded. */
    private LocalDateTime uploadedAt;

    public static AttachmentResponse from(TicketAttachment attachment) {
        AttachmentResponse response = new AttachmentResponse();
        response.id = attachment.getId();
        response.fileName = attachment.getFileName();
        response.filePath = attachment.getFilePath();
        response.uploadedAt = attachment.getUploadedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
