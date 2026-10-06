package com.sliit.helpdesk.category;

// Round Robin Assignment Strategy Test is part of the campus help desk category code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.category.service.RoundRobinAssignmentStrategy;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundRobinAssignmentStrategyTest {

    @Mock
    private UserRepository userRepository;
    private RoundRobinAssignmentStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RoundRobinAssignmentStrategy(userRepository);
    }

    @Test
    void roundRobinCyclesEvenlyAcrossSixTicketsAndThreeStaff() {
        User staffOne = staff(1L, "Alex Staff");
        User staffTwo = staff(2L, "Blair Staff");
        User staffThree = staff(3L, "Casey Staff");
        when(userRepository.findByEnabledTrueAndRoleInOrderByIdAsc(anyList()))
                .thenReturn(List.of(staffOne, staffTwo, staffThree));

        List<User> assignees = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Ticket ticket = new Ticket();
            ticket.setStatus(TicketStatus.OPEN);
            strategy.assign(ticket);
            assignees.add(ticket.getAssignee());
        }

        assertThat(assignees).containsExactly(
                staffOne, staffTwo, staffThree,
                staffOne, staffTwo, staffThree
        );
        assertThat(assignees.stream().filter(staffOne::equals).count()).isEqualTo(2);
        assertThat(assignees.stream().filter(staffTwo::equals).count()).isEqualTo(2);
        assertThat(assignees.stream().filter(staffThree::equals).count()).isEqualTo(2);
    }

    private static User staff(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setRole(Role.STAFF);
        user.setEnabled(true);
        return user;
    }
}
