package com.sliit.helpdesk.category.service;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.ticket.model.Ticket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Distributes new tickets evenly across available staff in a repeating cycle.
 */
@Component
@ConditionalOnProperty(name = "app.assignment.strategy", havingValue = "round-robin")
public class RoundRobinAssignmentStrategy implements AssignmentStrategy {

    static final List<Role> ASSIGNABLE = List.of(Role.STAFF, Role.ADMIN, Role.DEPT_HEAD);

    private final UserRepository userRepository;
    private final AtomicInteger cursor = new AtomicInteger(0);

    public RoundRobinAssignmentStrategy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void assign(Ticket ticket) {
        List<User> staff = userRepository.findByEnabledTrueAndRoleInOrderByIdAsc(ASSIGNABLE);
        if (staff.isEmpty()) {
            return;
        }
        int index = Math.floorMod(cursor.getAndIncrement(), staff.size());
        AssignmentStrategy.apply(ticket, staff.get(index));
    }
}
