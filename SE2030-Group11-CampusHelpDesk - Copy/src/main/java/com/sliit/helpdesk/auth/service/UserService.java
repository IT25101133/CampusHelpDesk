package com.sliit.helpdesk.auth.service;

// Registers, updates, and deactivates accounts. Admin accounts cannot be turned off.

import com.sliit.helpdesk.auth.dto.AdminUserRequest;
import com.sliit.helpdesk.auth.dto.ProfileUpdateRequest;
import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.auth.security.JwtUtil;
import com.sliit.helpdesk.knowledgebase.repository.ArticleRepository;
import com.sliit.helpdesk.notification.repository.CommentRepository;
import com.sliit.helpdesk.notification.repository.NotificationRepository;
import com.sliit.helpdesk.report.repository.AuditEventRepository;
import com.sliit.helpdesk.report.repository.AuditLogRepository;
import com.sliit.helpdesk.report.repository.ReportRepository;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ArticleRepository articleRepository;
    private final ReportRepository reportRepository;
    private final AuditEventRepository auditEventRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this(userRepository, passwordEncoder, jwtUtil, null, null, null, null, null, null, null);
    }

    @Autowired
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            TicketRepository ticketRepository,
            CommentRepository commentRepository,
            NotificationRepository notificationRepository,
            AuditLogRepository auditLogRepository,
            ArticleRepository articleRepository,
            ReportRepository reportRepository,
            AuditEventRepository auditEventRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.articleRepository = articleRepository;
        this.reportRepository = reportRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public User register(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        String email = user.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent() || userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account with that email already exists.");
        }
        user.setEmail(email);
        if (user.getFullName() != null) {
            user.setFullName(user.getFullName().trim());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(Role.STUDENT);
        }
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public String login(String email, String password) {
        User user = findByEmailOrThrow(email, "Invalid email or password");
        LoginHandler chain = new EnabledAccountHandler();
        chain.link(new PasswordMatchHandler(passwordEncoder));
        chain.handle(new LoginAttempt(user, password));
        return jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }

    @Transactional
    public User updateProfile(User user, ProfileUpdateRequest request) {
        user.setFullName(request.getFullName().trim());
        user.setDepartment(blankToNull(request.getDepartment()));
        user.setStudentId(blankToNull(request.getStudentId()));
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getNewPassword().length() < 8) {
                throw new IllegalArgumentException("New password must be at least 8 characters.");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        return userRepository.save(user);
    }

    public List<User> listAll() {
        return userRepository.findAll();
    }

    public User requireById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public User create(AdminUserRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole() == null ? Role.STUDENT : request.getRole());
        user.setDepartment(request.getDepartment());
        user.setStudentId(request.getStudentId());
        return register(user);
    }

    @Transactional
    public User update(Long userId, AdminUserRequest request) {
        User user = requireById(userId);
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new IllegalArgumentException("An account with that email already exists.");
            }
        });
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        if (user.getRole() == Role.ADMIN) {
            if (request.getRole() != null && request.getRole() != Role.ADMIN) {
                throw new AdminAccountProtectedException();
            }
            if (Boolean.FALSE.equals(request.getEnabled())) {
                throw new AdminAccountProtectedException();
            }
        }
        user.setRole(request.getRole() == null ? user.getRole() : request.getRole());
        user.setDepartment(blankToNull(request.getDepartment()));
        user.setStudentId(blankToNull(request.getStudentId()));
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters.");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        return userRepository.save(user);
    }

    @Transactional
    public User resetPassword(String email, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }
        User user = findByEmailOrThrow(email, "Reset token is invalid or expired.");
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public User deactivateAccount(Long userId) {
        return deactivateAccount(null, userId);
    }

    /**
     * Soft-delete. An admin account cannot be deactivated by anyone, including that admin.
     */
    @Transactional
    public User deactivateAccount(User actor, Long userId) {
        User user = requireById(userId);
        assertMayDeactivate(actor, user);
        user.setEnabled(false);
        user.setDeactivatedAt(LocalDateTime.now());
        user.setDeactivatedBy(actor == null ? null : actor.getId());
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long userId) {
        deactivateAccount(null, userId);
    }

    /**
     * Soft-delete unless {@code permanent} is set. Hard-delete refuses accounts that still own tickets or comments.
     */
    @Transactional
    public void deleteAccount(User actor, Long userId, boolean permanent) {
        if (permanent) {
            hardDelete(actor, userId);
            return;
        }
        deactivateAccount(actor, userId);
    }

    private void hardDelete(User actor, Long userId) {
        User user = requireById(userId);
        assertMayDeactivate(actor, user);
        if (ticketRepository == null || commentRepository == null) {
            throw new IllegalStateException("Account deletion is not fully configured.");
        }
        long tickets = ticketRepository.countByRequester_Id(userId) + ticketRepository.countByAssignee_Id(userId);
        long comments = commentRepository.countByAuthor_Id(userId);
        if (tickets > 0 || comments > 0) {
            throw new IllegalArgumentException("Account has linked records — use soft-delete instead");
        }
        if (notificationRepository != null) {
            notificationRepository.deleteByRecipientId(userId);
        }
        if (auditLogRepository != null) {
            auditLogRepository.detachUser(userId);
        }
        if (articleRepository != null) {
            articleRepository.detachAuthor(userId);
        }
        if (reportRepository != null) {
            reportRepository.detachCreator(userId);
            reportRepository.detachEditor(userId);
        }
        if (auditEventRepository != null) {
            auditEventRepository.detachUser(userId);
        }
        userRepository.deleteById(userId);
    }

    private void assertMayDeactivate(User actor, User target) {
        if (target.getRole() == Role.ADMIN) {
            throw new AdminAccountProtectedException();
        }
        if (actor == null) {
            return;
        }
        boolean self = actor.getId() != null && actor.getId().equals(target.getId());
        if (!self && actor.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("You cannot deactivate another user's account");
        }
    }

    public User requireByEmail(String email) {
        return findByEmailOrThrow(email, "User not found");
    }

    private User findByEmailOrThrow(String email, String notFoundMessage) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(notFoundMessage);
        }
        String normalized = email.trim().toLowerCase();
        return userRepository.findByEmail(normalized)
                .or(() -> userRepository.findByEmailIgnoreCase(email.trim()))
                .orElseThrow(() -> new IllegalArgumentException(notFoundMessage));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
