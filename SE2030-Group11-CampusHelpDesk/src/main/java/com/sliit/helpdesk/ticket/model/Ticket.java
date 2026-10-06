package com.sliit.helpdesk.ticket.model;

// Ticket is part of the campus help desk model code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.notification.model.Notification;
import com.sliit.helpdesk.notification.model.TicketComment;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TicketStatus status = TicketStatus.OPEN;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "submitter_role", length = 20)
    private Role submitterRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignee;

    @Column(name = "delete_requested")
    private Boolean deleteRequested = Boolean.FALSE;

    @Column(name = "delete_approved")
    private Boolean deleteApproved = Boolean.FALSE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "sla_due_at")
    private LocalDateTime slaDueAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "ticket")
    private List<Notification> notifications = new ArrayList<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (deleteRequested == null) {
            deleteRequested = Boolean.FALSE;
        }
        if (deleteApproved == null) {
            deleteApproved = Boolean.FALSE;
        }
    }

    public boolean isDeleteRequested() {
        return Boolean.TRUE.equals(deleteRequested);
    }

    public boolean isDeleteApproved() {
        return Boolean.TRUE.equals(deleteApproved);
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getTicketNumber() {
        return id == null ? "HD-NEW" : "HD-" + id;
    }
}
