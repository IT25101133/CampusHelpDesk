package com.sliit.helpdesk.config;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.category.repository.CategoryRepository;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Loads the sample ticket set used by the command center and insights dashboard
 * when the database has no tickets yet.
 */
@Component
@Order(2)
public class DemoTicketSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoTicketSeeder.class);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public DemoTicketSeeder(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (ticketRepository.count() > 0) {
            return;
        }
        int created = 0;
        created += save(
                "Cannot sign in to the LMS",
                "Moodle returns an invalid credentials error after the password reset from last week. Student ID IT20201234.",
                TicketStatus.IN_PROGRESS, TicketPriority.HIGH,
                "IT Support", "student@sliit.lk", "it.support@sliit.lk",
                LocalDateTime.of(2026, 9, 8, 9, 15),
                LocalDateTime.of(2026, 9, 9, 14, 2),
                null
        );
        created += save(
                "Broken projector in Lab B-204",
                "The ceiling projector flickers and shuts off after about five minutes. Happened during the SE2030 lecture.",
                TicketStatus.OPEN, TicketPriority.MEDIUM,
                "Facilities", "lecturer@sliit.lk", "staff@sliit.lk",
                LocalDateTime.of(2026, 9, 9, 11, 40),
                LocalDateTime.of(2026, 9, 9, 11, 40),
                null
        );
        created += save(
                "Request a fee payment receipt",
                "Need an official receipt for the semester 1 installment paid on 01 Sep 2026 for a visa extension.",
                TicketStatus.RESOLVED, TicketPriority.LOW,
                "Finance Office", "student@sliit.lk", "admin@sliit.lk",
                LocalDateTime.of(2026, 9, 4, 16, 20),
                LocalDateTime.of(2026, 9, 6, 10, 5),
                LocalDateTime.of(2026, 9, 6, 10, 5)
        );
        created += save(
                "Moodle assignment upload fails",
                "SE2030 Assignment 2 PDF will not upload. Moodle shows a 413 error after about 30 seconds.",
                TicketStatus.CLOSED, TicketPriority.MEDIUM,
                "IT Support", "student@sliit.lk", "it.support@sliit.lk",
                LocalDateTime.of(2026, 8, 28, 10, 5),
                LocalDateTime.of(2026, 8, 30, 16, 40),
                LocalDateTime.of(2026, 8, 30, 16, 40)
        );
        created += save(
                "Air conditioning in Lecture Hall A-101",
                "The hall is above 30C by mid-morning. Last Friday lecture had to be shortened.",
                TicketStatus.OPEN, TicketPriority.HIGH,
                "Facilities", "lecturer@sliit.lk", "staff@sliit.lk",
                LocalDateTime.of(2026, 9, 1, 8, 10),
                LocalDateTime.of(2026, 9, 1, 8, 10),
                null
        );
        created += save(
                "Grade review for SE2030 CA",
                "CA1 mark on Courseweb is 12/20 but the returned script shows 16/20.",
                TicketStatus.IN_PROGRESS, TicketPriority.MEDIUM,
                "Academic", "student@sliit.lk", "head@sliit.lk",
                LocalDateTime.of(2026, 9, 7, 13, 25),
                LocalDateTime.of(2026, 9, 8, 9, 0),
                null
        );
        created += save(
                "Scholarship stipend not received",
                "August merit scholarship did not reach the registered bank account.",
                TicketStatus.RESOLVED, TicketPriority.HIGH,
                "Finance Office", "student@sliit.lk", "admin@sliit.lk",
                LocalDateTime.of(2026, 8, 25, 11, 0),
                LocalDateTime.of(2026, 8, 27, 15, 30),
                LocalDateTime.of(2026, 8, 27, 15, 30)
        );
        if (created > 0) {
            log.info("Seeded {} demo tickets for the dashboards", created);
        }
    }

    private int save(
            String title,
            String description,
            TicketStatus status,
            TicketPriority priority,
            String categoryName,
            String requesterEmail,
            String assigneeEmail,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime resolvedAt
    ) {
        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
        User requester = userRepository.findByEmailIgnoreCase(requesterEmail).orElse(null);
        User assignee = userRepository.findByEmailIgnoreCase(assigneeEmail).orElse(null);
        if (category == null || requester == null) {
            return 0;
        }
        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setStatus(status);
        ticket.setPriority(priority);
        ticket.setCategory(category);
        ticket.setRequester(requester);
        ticket.setSubmitterRole(requester.getRole());
        ticket.setAssignee(assignee);
        ticket.setCreatedAt(createdAt);
        ticket.setUpdatedAt(updatedAt);
        ticket.setResolvedAt(resolvedAt);
        if (category.getSlaHours() > 0) {
            ticket.setSlaDueAt(createdAt.plusHours(category.getSlaHours()));
        }
        ticketRepository.save(ticket);
        return 1;
    }
}
