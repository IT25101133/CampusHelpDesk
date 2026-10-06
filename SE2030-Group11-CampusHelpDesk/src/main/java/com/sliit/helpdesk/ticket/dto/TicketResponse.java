package com.sliit.helpdesk.ticket.dto;

// Ticket Response is part of the campus help desk dto code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** READ body. One ticket sent back to the page or the API. */
public class TicketResponse {

    /** Database id. */
    private Long id;
    /** Display number such as HD-12. */
    private String ticketNumber;
    /** Short name of the problem. */
    private String title;
    /** Full description. */
    private String description;
    /** Open, In Progress, Resolved, or Closed. */
    private TicketStatus status;
    /** How urgent the ticket is. */
    private TicketPriority priority;
    /** Department id. */
    private Long categoryId;
    /** Department name, such as IT Support. */
    private String category;
    /** Faculty or office that owns the category. */
    private String department;
    /** Name of the person who opened the ticket. */
    private String requester;
    /** Student or lecturer. */
    private Role submitterRole;
    /** Name of the staff member working on it. Empty when nobody is assigned. */
    private String assignee;
    /** Id of that staff member. */
    private Long assigneeId;
    /** True when the owner has asked for the ticket to be deleted. */
    private boolean deleteRequested;
    /** True when staff have allowed that delete. */
    private boolean deleteApproved;
    /** When the ticket was submitted. */
    private LocalDateTime createdAt;
    /** When it was last changed. */
    private LocalDateTime updatedAt;
    /** When it was resolved or closed. */
    private LocalDateTime resolvedAt;
    /** When the reply is due under the SLA. */
    private LocalDateTime slaDueAt;
    /** Id of the person who opened the ticket. */
    private Long requesterId;
    /** Replies on this ticket. */
    private List<CommentResponse> comments = new ArrayList<>();
    /** Files uploaded on this ticket. */
    private List<AttachmentResponse> attachments = new ArrayList<>();
    /** True for staff, admin, and department head. */
    private boolean canManage;
    /** True when this user may change the title, description, or category. */
    private boolean canEdit;
    /** True when this user may delete the ticket. */
    private boolean canDelete;
    /** True when this user may ask staff to allow deletion. */
    private boolean canRequestDelete;
    /** True when this user may approve a delete request. */
    private boolean canApproveDelete;
    /** True when the owner may accept a resolved ticket and close it. */
    private boolean canConfirmClose;
    /** True when the owner may send a resolved ticket back to the same assignee. */
    private boolean canReopenResolved;
    /** True when staff may close a resolved ticket with a written reason. */
    private boolean canStaffClose;
    /** True when this user may reopen a closed ticket. */
    private boolean canReopen;
    /** True when this user may add a reply. */
    private boolean canComment;
    /** Message shown when a closed ticket can no longer be reopened. */
    private String closedNotice;
    /** Staff names for the assign dropdown. Empty for students and lecturers. */
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
            response.department = ticket.getCategory().getDepartment();
        }
        if (ticket.getRequester() != null) {
            response.requesterId = ticket.getRequester().getId();
            response.requester = ticket.getRequester().getFullName();
        }
        response.submitterRole = ticket.getSubmitterRole();
        response.deleteRequested = ticket.isDeleteRequested();
        response.deleteApproved = ticket.isDeleteApproved();
        if (ticket.getAssignee() != null) {
            response.assignee = ticket.getAssignee().getFullName();
            response.assigneeId = ticket.getAssignee().getId();
        }
        response.createdAt = ticket.getCreatedAt();
        response.updatedAt = ticket.getUpdatedAt();
        response.resolvedAt = ticket.getResolvedAt();
        response.slaDueAt = ticket.getSlaDueAt();
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

    public String getDepartment() {
        return department;
    }

    public String getRequester() {
        return requester;
    }

    public Role getSubmitterRole() {
        return submitterRole;
    }

    public String getAssignee() {
        return assignee;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public boolean isDeleteRequested() {
        return deleteRequested;
    }

    public boolean isDeleteApproved() {
        return deleteApproved;
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

    public LocalDateTime getSlaDueAt() {
        return slaDueAt;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public List<CommentResponse> getComments() {
        return comments;
    }

    public void setComments(List<CommentResponse> comments) {
        this.comments = comments;
    }

    public List<AttachmentResponse> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentResponse> attachments) {
        this.attachments = attachments;
    }

    public boolean isCanManage() {
        return canManage;
    }

    public void setCanManage(boolean canManage) {
        this.canManage = canManage;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    public boolean isCanRequestDelete() {
        return canRequestDelete;
    }

    public void setCanRequestDelete(boolean canRequestDelete) {
        this.canRequestDelete = canRequestDelete;
    }

    public boolean isCanApproveDelete() {
        return canApproveDelete;
    }

    public void setCanApproveDelete(boolean canApproveDelete) {
        this.canApproveDelete = canApproveDelete;
    }

    public boolean isCanReopen() {
        return canReopen;
    }

    public void setCanReopen(boolean canReopen) {
        this.canReopen = canReopen;
    }

    public boolean isCanComment() {
        return canComment;
    }

    public void setCanComment(boolean canComment) {
        this.canComment = canComment;
    }

    public boolean isCanConfirmClose() {
        return canConfirmClose;
    }

    public void setCanConfirmClose(boolean canConfirmClose) {
        this.canConfirmClose = canConfirmClose;
    }

    public boolean isCanReopenResolved() {
        return canReopenResolved;
    }

    public void setCanReopenResolved(boolean canReopenResolved) {
        this.canReopenResolved = canReopenResolved;
    }

    public boolean isCanStaffClose() {
        return canStaffClose;
    }

    public void setCanStaffClose(boolean canStaffClose) {
        this.canStaffClose = canStaffClose;
    }

    public String getClosedNotice() {
        return closedNotice;
    }

    public void setClosedNotice(String closedNotice) {
        this.closedNotice = closedNotice;
    }

    public List<StaffMemberResponse> getStaffMembers() {
        return staffMembers;
    }

    public void setStaffMembers(List<StaffMemberResponse> staffMembers) {
        this.staffMembers = staffMembers;
    }
}
