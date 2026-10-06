package com.sliit.helpdesk.category.service;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssignmentService {

    private static final List<Role> ASSIGNABLE = List.of(Role.STAFF, Role.ADMIN, Role.DEPT_HEAD);

    private final UserRepository userRepository;

    public AssignmentService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void assignDefaultStaff(Ticket ticket, Category category) {
        if (category.getDepartment() == null || category.getDepartment().isBlank()) {
            return;
        }
        List<User> staff = userRepository.findByDepartmentIgnoreCaseAndRoleInOrderByFullNameAsc(
                category.getDepartment(),
                ASSIGNABLE
        );
        if (!staff.isEmpty()) {
            ticket.setAssignee(staff.get(0));
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
    }
}
