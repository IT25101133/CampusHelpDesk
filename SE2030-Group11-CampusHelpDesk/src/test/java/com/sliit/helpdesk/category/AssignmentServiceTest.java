package com.sliit.helpdesk.category;

// Assignment Service Test is part of the campus help desk category code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.category.service.CategoryBasedAssignmentStrategy;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private UserRepository userRepository;
    private CategoryBasedAssignmentStrategy assignmentStrategy;

    @BeforeEach
    void setUp() {
        assignmentStrategy = new CategoryBasedAssignmentStrategy(userRepository);
    }

    @Test
    void assignDefaultStaffSetsAssigneeAndMovesToInProgress() {
        User staff = new User();
        staff.setId(9L);
        staff.setRole(Role.STAFF);
        staff.setEnabled(true);
        Category category = new Category();
        category.setDepartment("IT Services");
        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCategory(category);
        when(userRepository.findByDepartmentIgnoreCaseAndRoleInOrderByFullNameAsc(eq("IT Services"), anyList()))
                .thenReturn(List.of(staff));

        assignmentStrategy.assign(ticket);

        assertThat(ticket.getAssignee()).isEqualTo(staff);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void assignDefaultStaffLeavesTicketWhenNoAssignee() {
        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCategory(new Category());

        assignmentStrategy.assign(ticket);

        assertThat(ticket.getAssignee()).isNull();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
    }
}
