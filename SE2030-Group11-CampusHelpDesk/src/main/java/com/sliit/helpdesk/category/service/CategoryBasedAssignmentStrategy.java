package com.sliit.helpdesk.category.service;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.ticket.model.Ticket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Assigns tickets to staff whose department is tagged on the ticket category.
 */
@Component
@ConditionalOnProperty(name = "app.assignment.strategy", havingValue = "category-based", matchIfMissing = true)
public class CategoryBasedAssignmentStrategy implements AssignmentStrategy {

    static final List<Role> ASSIGNABLE = List.of(Role.STAFF, Role.ADMIN, Role.DEPT_HEAD);

    private final UserRepository userRepository;

    public CategoryBasedAssignmentStrategy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void assign(Ticket ticket) {
        Category category = ticket.getCategory();
        if (category == null) {
            return;
        }
        User assignee = firstTaggedStaff(category);
        if (assignee != null) {
            AssignmentStrategy.apply(ticket, assignee);
        }
    }

    private User firstTaggedStaff(Category category) {
        User fromDepartment = firstEnabled(staffInDepartment(category.getDepartment()));
        if (fromDepartment != null) {
            return fromDepartment;
        }
        return firstEnabled(staffInDepartment(category.getName()));
    }

    private List<User> staffInDepartment(String department) {
        if (department == null || department.isBlank()) {
            return List.of();
        }
        return userRepository.findByDepartmentIgnoreCaseAndRoleInOrderByFullNameAsc(department.trim(), ASSIGNABLE);
    }

    private static User firstEnabled(List<User> staff) {
        return staff.stream().filter(User::isEnabled).findFirst().orElse(null);
    }
}
