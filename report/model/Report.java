package com.sliit.helpdesk.report.model;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A saved snapshot of ticket figures for one filter combination.
 * A null {@code department}, {@code status}, or {@code priority} means that dimension was not filtered.
 */
@Getter
@Setter
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private TicketPriority priority;

    @Column(name = "ticket_count")
    private long ticketCount;

    @Column(name = "resolved_count")
    private long resolvedCount;

    @Column(name = "avg_resolution_hours")
    private Double avgResolutionHours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Set when an admin or the original creator edits the saved report. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public String getDepartmentLabel() {
        return department == null ? "All departments" : department;
    }

    public String getStatusLabel() {
        return status == null ? "Any status" : status.name().replace('_', ' ');
    }

    public String getPriorityLabel() {
        return priority == null ? "Any priority" : priority.name();
    }
}
