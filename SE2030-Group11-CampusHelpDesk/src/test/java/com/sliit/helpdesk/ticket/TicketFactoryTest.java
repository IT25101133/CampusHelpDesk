package com.sliit.helpdesk.ticket;

// Ticket Factory Test is part of the campus help desk ticket code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.ticket.dto.TicketForm;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.service.SlaPolicy;
import com.sliit.helpdesk.ticket.service.TicketFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TicketFactoryTest {

    @Test
    void slaPolicyIsASingleSharedInstance() {
        assertThat(SlaPolicy.getInstance()).isSameAs(SlaPolicy.getInstance());
    }

    @Test
    void priorityScalesTheCategorySlaWindow() {
        SlaPolicy policy = SlaPolicy.getInstance();

        assertThat(policy.resolveHours(48, TicketPriority.CRITICAL)).isEqualTo(12);
        assertThat(policy.resolveHours(48, TicketPriority.HIGH)).isEqualTo(24);
        assertThat(policy.resolveHours(48, TicketPriority.MEDIUM)).isEqualTo(48);
        assertThat(policy.resolveHours(48, TicketPriority.LOW)).isEqualTo(96);
        assertThat(policy.resolveHours(0, null)).isEqualTo(48);
    }

    @Test
    void studentAndStaffCreatorsOpenUnassignedTickets() {
        TicketFactory factory = new TicketFactory();
        Category category = new Category();
        category.setName("IT Support");
        category.setSlaHours(48);
        TicketForm form = form(TicketPriority.HIGH);

        Ticket studentTicket = factory.create(user(Role.STUDENT), form, category);
        Ticket staffTicket = factory.create(user(Role.STAFF), form, category);

        assertThat(studentTicket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(studentTicket.getAssignee()).isNull();
        assertThat(studentTicket.getSubmitterRole()).isEqualTo(Role.STUDENT);
        assertThat(studentTicket.getSlaDueAt()).isAfter(LocalDateTime.now().plusHours(23));

        assertThat(staffTicket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(staffTicket.getAssignee()).isNull();
        assertThat(staffTicket.getSubmitterRole()).isEqualTo(Role.STAFF);
        assertThat(staffTicket.getPriority()).isEqualTo(TicketPriority.HIGH);
    }

    @Test
    void missingRoleFallsBackByCreator() {
        TicketFactory factory = new TicketFactory();
        Category category = new Category();
        category.setSlaHours(24);

        Ticket external = factory.create(null, form(TicketPriority.LOW), category);
        User unnamedStaff = new User();
        unnamedStaff.setRole(Role.ADMIN);
        Ticket internal = factory.create(unnamedStaff, form(TicketPriority.CRITICAL), category);

        assertThat(external.getSubmitterRole()).isEqualTo(Role.STUDENT);
        assertThat(internal.getSubmitterRole()).isEqualTo(Role.ADMIN);
        assertThat(internal.getPriority()).isEqualTo(TicketPriority.CRITICAL);
    }

    private static User user(Role role) {
        User user = new User();
        user.setId(1L);
        user.setRole(role);
        user.setFullName("Campus User");
        return user;
    }

    private static TicketForm form(TicketPriority priority) {
        TicketForm form = new TicketForm();
        form.setTitle("  LMS down  ");
        form.setDescription("  Cannot open CourseWeb  ");
        form.setPriority(priority);
        return form;
    }
}
