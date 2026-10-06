package com.sliit.helpdesk.report.service;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.category.repository.CategoryRepository;
import com.sliit.helpdesk.report.dto.TicketReportCriteria;
import com.sliit.helpdesk.report.dto.TicketReportCriteriaBuilder;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds the filtered ticket list with JPA criteria so user input is never concatenated into JPQL.
 * The report page and the Excel export both call {@link #find}.
 */
@Service
public class TicketReportQuery {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TicketReportQuery(
            TicketRepository ticketRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Ticket> find(
            Long categoryId,
            String department,
            List<TicketStatus> statuses,
            List<TicketPriority> priorities,
            LocalDate from,
            LocalDate to,
            Long assignedToId,
            Long createdById
    ) {
        TicketReportCriteria criteria = validate(categoryId, department, statuses, priorities, from, to, assignedToId, createdById);
        return ticketRepository.findAll(specification(criteria), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public TicketReportCriteria describe(
            Long categoryId,
            String department,
            List<TicketStatus> statuses,
            List<TicketPriority> priorities,
            LocalDate from,
            LocalDate to,
            Long assignedToId,
            Long createdById
    ) {
        return validate(categoryId, department, statuses, priorities, from, to, assignedToId, createdById);
    }

    /**
     * Dropdown values for the report filter form. Departments come from known category and user rows.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> filterOptions() {
        List<Map<String, Object>> categories = new ArrayList<>();
        Set<String> departments = new LinkedHashSet<>();
        for (Category category : categoryRepository.findAllByOrderByNameAsc()) {
            String label = category.getDepartment() == null || category.getDepartment().isBlank()
                    || category.getDepartment().equalsIgnoreCase(category.getName())
                    ? category.getName()
                    : category.getName() + " — " + category.getDepartment();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", category.getId());
            row.put("name", category.getName());
            row.put("department", category.getDepartment());
            row.put("label", label);
            categories.add(row);
            if (category.getDepartment() != null && !category.getDepartment().isBlank()) {
                departments.add(category.getDepartment().trim());
            }
        }
        for (String department : userRepository.findDistinctDepartments()) {
            if (department != null && !department.isBlank()) {
                departments.add(department.trim());
            }
        }
        List<Map<String, Object>> people = userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", user.getId());
                    row.put("fullName", user.getFullName());
                    row.put("role", user.getRole() == null ? null : user.getRole().name());
                    return row;
                })
                .toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("categories", categories);
        body.put("departments", new ArrayList<>(departments));
        body.put("statuses", List.of(TicketStatus.values()));
        body.put("priorities", List.of(TicketPriority.values()));
        body.put("assignees", people);
        body.put("creators", people);
        return body;
    }

    private TicketReportCriteria validate(
            Long categoryId,
            String department,
            List<TicketStatus> statuses,
            List<TicketPriority> priorities,
            LocalDate from,
            LocalDate to,
            Long assignedToId,
            Long createdById
    ) {
        TicketReportCriteriaBuilder criteriaBuilder = new TicketReportCriteriaBuilder();
        if (categoryId == null) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (blankToNull(department) == null) {
            throw new IllegalArgumentException("Department is required.");
        }
        if (statuses == null || statuses.stream().noneMatch(java.util.Objects::nonNull)) {
            throw new IllegalArgumentException("Status is required.");
        }
        if (priorities == null || priorities.stream().noneMatch(java.util.Objects::nonNull)) {
            throw new IllegalArgumentException("Priority is required.");
        }
        if (from == null) {
            throw new IllegalArgumentException("From date is required.");
        }
        if (to == null) {
            throw new IllegalArgumentException("To date is required.");
        }
        if (assignedToId == null) {
            throw new IllegalArgumentException("Assigned to is required.");
        }
        if (createdById == null) {
            throw new IllegalArgumentException("Created by is required.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date must be on or before the to date.");
        }
        if (from != null && to != null && from.plusYears(5).isBefore(to)) {
            // Wording avoids "cannot", which the API error mapper treats as a permission failure.
            throw new IllegalArgumentException("Date range must be 5 years or less.");
        }
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown category."));
            String label = category.getDepartment() == null || category.getDepartment().isBlank()
                    ? category.getName()
                    : category.getName() + " — " + category.getDepartment();
            criteriaBuilder.category(category.getId(), label);
        }
        String scopedDepartment = blankToNull(department);
        if (scopedDepartment != null && !knownDepartment(scopedDepartment)) {
            throw new IllegalArgumentException("Unknown department.");
        }
        criteriaBuilder.department(scopedDepartment);
        criteriaBuilder.statuses(statuses == null ? List.of() : statuses.stream().filter(java.util.Objects::nonNull).toList());
        criteriaBuilder.priorities(priorities == null ? List.of() : priorities.stream().filter(java.util.Objects::nonNull).toList());
        if (from != null) {
            criteriaBuilder.from(from.atStartOfDay());
        }
        if (to != null) {
            criteriaBuilder.to(to.atTime(LocalTime.MAX));
        }
        if (assignedToId != null) {
            User assignee = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown assignee."));
            criteriaBuilder.assignedTo(assignee.getId(), assignee.getFullName());
        }
        if (createdById != null) {
            User creator = userRepository.findById(createdById)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown creator."));
            criteriaBuilder.createdBy(creator.getId(), creator.getFullName());
        }
        return criteriaBuilder.build();
    }

    private boolean knownDepartment(String department) {
        String needle = department.toLowerCase(Locale.ROOT);
        boolean onCategory = categoryRepository.findAll().stream()
                .map(Category::getDepartment)
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(value -> value.trim().toLowerCase(Locale.ROOT).equals(needle));
        if (onCategory) {
            return true;
        }
        return userRepository.findDistinctDepartments().stream()
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(value -> value.trim().toLowerCase(Locale.ROOT).equals(needle));
    }

    /**
     * AND across filters. Status and priority use IN when more than one value is selected.
     * The date window is inclusive of the whole from-day and to-day.
     */
    static Specification<Ticket> specification(TicketReportCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Ticket, ?> category = root.join("category", JoinType.LEFT);
            Join<Ticket, ?> requester = root.join("requester", JoinType.LEFT);
            Join<Ticket, ?> assignee = root.join("assignee", JoinType.LEFT);
            if (criteria.getCategoryId() != null) {
                predicates.add(cb.equal(category.get("id"), criteria.getCategoryId()));
            }
            if (criteria.getDepartment() != null) {
                String department = criteria.getDepartment().toLowerCase(Locale.ROOT);
                predicates.add(cb.or(
                        cb.equal(cb.lower(category.get("department")), department),
                        cb.equal(cb.lower(requester.get("department")), department)
                ));
            }
            if (!criteria.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(criteria.getStatuses()));
            }
            if (!criteria.getPriorities().isEmpty()) {
                predicates.add(root.get("priority").in(criteria.getPriorities()));
            }
            if (criteria.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getFrom()));
            }
            if (criteria.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getTo()));
            }
            if (criteria.getAssignedToId() != null) {
                predicates.add(cb.equal(assignee.get("id"), criteria.getAssignedToId()));
            }
            if (criteria.getCreatedById() != null) {
                predicates.add(cb.equal(requester.get("id"), criteria.getCreatedById()));
            }
            query.distinct(true);
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
