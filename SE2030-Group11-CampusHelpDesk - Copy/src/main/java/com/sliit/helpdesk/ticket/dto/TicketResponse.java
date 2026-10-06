package com.sliit.helpdesk.ticket.dto;

import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketResponse {

    private Long id;
    private String ticketNumber;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Long categoryId;
    private String category;
    private String requester;
    private String assignee;
    private Long assigneeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private List<CommentResponse> comments = new ArrayList<>();
    private boolean canManage;
    private List<StaffMemberResponse> staffMembers = new ArrayList<>();

    public static TicketResponse from(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.id = ticket.getId();
        response.ticketNumber = ticket.getTicketNumber();
        response.title = ticket.getTitle();
        response.description = ticket.getDescription();
        response.status = ticket.getStatus();
        response.priority = ticket.getPriority();
        if (ticket.getCategory() != null) {
            response.categoryId = ticket.getCategory().getId();
            response.category = ticket.getCategory().getName();
        }
        if (ticket.getRequester() != null) {
            response.requester = ticket.getRequester().getFullName();
        }
        if (ticket.getAssignee() != null) {
            response.assignee = ticket.getAssignee().getFullName();
            response.assigneeId = ticket.getAssignee().getId();
        }
        response.createdAt = ticket.getCreatedAt();
        response.updatedAt = ticket.getUpdatedAt();
        response.resolvedAt = ticket.getResolvedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategory() {
        return category;
    }

    public String getRequester() {
        return requester;
    }

    public String getAssignee() {
        return assignee;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public List<CommentResponse> getComments() {
        return comments;
    }

    public void setComments(List<CommentResponse> comments) {
        this.comments = comments;
    }

    public boolean isCanManage() {
        return canManage;
    }

    public void setCanManage(boolean canManage) {
        this.canManage = canManage;
    }

    public List<StaffMemberResponse> getStaffMembers() {
        return staffMembers;
    }

    public void setStaffMembers(List<StaffMemberResponse> staffMembers) {
        this.staffMembers = staffMembers;
    }
}
