package com.sliit.helpdesk.ticket.service;

// Creates, updates, assigns, and closes tickets, then tells the notification listener.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.category.service.CategoryService;
import com.sliit.helpdesk.notification.TicketEventListener;
import com.sliit.helpdesk.notification.model.TicketComment;
import com.sliit.helpdesk.notification.service.CommentService;
import com.sliit.helpdesk.notification.service.NotificationService;
import com.sliit.helpdesk.report.service.AuditService;
import com.sliit.helpdesk.ticket.dto.TicketForm;
import com.sliit.helpdesk.ticket.dto.TicketUpdateRequest;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketAttachment;
import com.sliit.helpdesk.ticket.model.TicketCategories;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketAttachmentRepository;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TicketService {

    /** Disk-cleanup failures go here so a later janitor pass can retry them. */
    private static final Logger CLEANUP_LOG = LoggerFactory.getLogger("attachment.cleanup");

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "application/pdf"
    );

    private final TicketRepository ticketRepository;
    private final CommentService commentService;
    private final TicketAttachmentRepository attachmentRepository;
    private final CategoryService categoryService;
    private final NotificationService notificationService;
    private final List<TicketEventListener> ticketEventListeners;
    private final AuthService authService;
    private final AuditService auditService;
    private final TicketFactory ticketFactory;
    private final Path uploadRoot;
    private final int maxAttachmentMb;

    @Autowired
    public TicketService(
            TicketRepository ticketRepository,
            CommentService commentService,
            TicketAttachmentRepository attachmentRepository,
            CategoryService categoryService,
            NotificationService notificationService,
            List<TicketEventListener> ticketEventListeners,
            AuthService authService,
            AuditService auditService,
            TicketFactory ticketFactory,
            @Value("${app.uploads.dir:src/main/resources/static/uploads}") String uploadDir,
            @Value("${app.uploads.max-file-size-mb:" + TicketAttachmentConstants.MAX_TICKET_ATTACHMENT_MB + "}") int maxAttachmentMb
    ) {
        this.ticketRepository = ticketRepository;
        this.commentService = commentService;
        this.attachmentRepository = attachmentRepository;
        this.categoryService = categoryService;
        this.notificationService = notificationService;
        this.ticketEventListeners = ticketEventListeners;
        this.authService = authService;
        this.auditService = auditService;
        this.ticketFactory = ticketFactory;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.maxAttachmentMb = maxAttachmentMb > 0 ? maxAttachmentMb : TicketAttachmentConstants.MAX_TICKET_ATTACHMENT_MB;
    }

    public TicketService(
            TicketRepository ticketRepository,
            CommentService commentService,
            TicketAttachmentRepository attachmentRepository,
            CategoryService categoryService,
            NotificationService notificationService,
            List<TicketEventListener> ticketEventListeners,
            AuthService authService,
            AuditService auditService,
            TicketFactory ticketFactory,
            String uploadDir
    ) {
        this(ticketRepository, commentService, attachmentRepository, categoryService, notificationService,
                ticketEventListeners, authService, auditService, ticketFactory, uploadDir, TicketAttachmentConstants.MAX_TICKET_ATTACHMENT_MB);
    }

    public int getMaxAttachmentMb() {
        return maxAttachmentMb;
    }

    public long getMaxAttachmentBytes() {
        return (long) maxAttachmentMb * 1024L * 1024L;
    }

    /** READ: tickets this user is allowed to see. Submitters only see their own. */
    public List<Ticket> listFor(User user) {
        return listFor(user, null, null, null, null, null);
    }

    public List<Ticket> listFor(User user, TicketStatus status, TicketPriority priority, Long createdBy) {
        return listFor(user, status, priority, createdBy, null, null);
    }

    public List<Ticket> listFor(
            User user,
            TicketStatus status,
            TicketPriority priority,
            Long createdBy,
            String query,
            Long categoryId
    ) {
        Long requesterId = createdBy;
        // A student or lecturer only sees tickets they opened.
        if (isSubmitter(user)) {
            requesterId = user.getId();
        }
        Long filterRequesterId = requesterId;
        String needle = query == null ? null : query.trim().toLowerCase(Locale.ROOT);
        Specification<Ticket> spec = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (filterRequesterId != null) {
                predicates.add(cb.equal(root.get("requester").get("id"), filterRequesterId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (needle != null && !needle.isBlank()) {
                List<Predicate> search = new ArrayList<>();
                search.add(cb.like(cb.lower(root.get("title")), "%" + needle + "%"));
                search.add(cb.like(cb.lower(root.get("description")), "%" + needle + "%"));
                if (needle.startsWith("hd-")) {
                    String digits = needle.substring(3).replaceAll("[^0-9]", "");
                    if (!digits.isBlank()) {
                        try {
                            search.add(cb.equal(root.get("id"), Long.parseLong(digits)));
                        } catch (NumberFormatException ignored) {
                            // fall through to text search
                        }
                    }
                }
                predicates.add(cb.or(search.toArray(Predicate[]::new)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /** READ: one ticket, or an error if it is missing or hidden from this user. */
    public Ticket requireVisible(Long id, User user) {
        // Load the row. Missing id means the ticket does not exist.
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        // Students and lecturers cannot open someone else's ticket.
        if (isSubmitter(user) && (ticket.getRequester() == null || !ticket.getRequester().getId().equals(user.getId()))) {
            throw new IllegalArgumentException("You cannot view this ticket.");
        }
        return ticket;
    }

    public boolean isHelpDeskStaff(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return switch (user.getRole()) {
            case STAFF, ADMIN, DEPT_HEAD -> true;
            case STUDENT, LECTURER -> false;
        };
    }

    public boolean isSubmitter(User user) {
        return !isHelpDeskStaff(user);
    }

    public static final String CLOSED_FOLLOW_UP =
            "This ticket is closed. Please submit a new ticket if the issue persists.";

    /** Days a requester has to confirm or reopen a resolved ticket before it closes itself. */
    public static final int RESOLVED_RESPONSE_DAYS = 5;

    public boolean isOpenPhase(Ticket ticket) {
        return ticket.getStatus() == TicketStatus.OPEN && ticket.getAssignee() == null;
    }

    public boolean isTerminal(Ticket ticket) {
        return ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED;
    }

    /** Title, description, and category while the ticket is still open and unassigned. */
    public boolean canEditTicket(Ticket ticket, User user) {
        if (ticket == null || user == null) {
            return false;
        }
        if (isSubmitter(user)) {
            return owns(ticket, user) && isOpenPhase(ticket);
        }
        return isHelpDeskStaff(user) && !isTerminal(ticket);
    }

    /**
     * Owners may delete an open, unassigned ticket, or any of their tickets after staff approve a delete request.
     * An administrator may delete a ticket in any status.
     */
    public boolean canDeleteTicket(Ticket ticket, User user) {
        if (ticket == null || user == null || user.getRole() == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!isSubmitter(user) || !owns(ticket, user)) {
            return false;
        }
        if (isOpenPhase(ticket)) {
            return true;
        }
        return ticket.isDeleteRequested() && ticket.isDeleteApproved();
    }

    /** Owner can ask staff to allow deletion once the ticket is no longer open and unassigned. */
    public boolean canRequestDelete(Ticket ticket, User user) {
        if (ticket == null || user == null) {
            return false;
        }
        return isSubmitter(user)
                && owns(ticket, user)
                && !isOpenPhase(ticket)
                && !ticket.isDeleteRequested()
                && !ticket.isDeleteApproved();
    }

    /** Staff can approve a delete request that is still waiting. */
    public boolean canApproveDelete(Ticket ticket, User user) {
        return ticket != null
                && isHelpDeskStaff(user)
                && ticket.isDeleteRequested()
                && !ticket.isDeleteApproved();
    }

    /** The person who opened a resolved ticket can accept the fix and close it. */
    public boolean canConfirmClose(Ticket ticket, User user) {
        return ticket != null
                && user != null
                && ticket.getStatus() == TicketStatus.RESOLVED
                && isSubmitter(user)
                && owns(ticket, user);
    }

    /** The owner can send a resolved ticket back to the same assignee. */
    public boolean canReopenResolved(Ticket ticket, User user) {
        return canConfirmClose(ticket, user);
    }

    /** Staff can close a resolved ticket with a written reason. */
    public boolean canStaffClose(Ticket ticket, User user) {
        return ticket != null
                && ticket.getStatus() == TicketStatus.RESOLVED
                && isHelpDeskStaff(user);
    }

    public boolean canReopenTicket(Ticket ticket, User user) {
        if (ticket == null || user == null || ticket.getStatus() != TicketStatus.CLOSED) {
            return false;
        }
        if (isHelpDeskStaff(user)) {
            return true;
        }
        return isSubmitter(user) && owns(ticket, user) && closedWithinWindow(ticket);
    }

    public boolean canCommentOn(Ticket ticket, User user) {
        if (ticket == null || user == null) {
            return false;
        }
        if (isHelpDeskStaff(user)) {
            return true;
        }
        return owns(ticket, user) && !isTerminal(ticket);
    }

    public String closedNotice(Ticket ticket, User user) {
        if (ticket == null || user == null || ticket.getStatus() != TicketStatus.CLOSED || isHelpDeskStaff(user)) {
            return null;
        }
        if (isSubmitter(user) && owns(ticket, user) && !closedWithinWindow(ticket)) {
            return CLOSED_FOLLOW_UP;
        }
        return null;
    }

    private boolean closedWithinWindow(Ticket ticket) {
        LocalDateTime closedAt = ticket.getResolvedAt() != null ? ticket.getResolvedAt() : ticket.getUpdatedAt();
        if (closedAt == null) {
            return false;
        }
        return !closedAt.isBefore(LocalDateTime.now().minusDays(7));
    }

    public List<Category> selectableCategories() {
        Map<String, Category> byName = new LinkedHashMap<>();
        for (Category category : categoryService.activeCategories()) {
            if (category != null && category.getName() != null) {
                byName.putIfAbsent(category.getName().trim().toLowerCase(Locale.ROOT), category);
            }
        }
        List<Category> ordered = new ArrayList<>();
        for (String name : TicketCategories.NAMES) {
            Category category = byName.get(name.toLowerCase(Locale.ROOT));
            if (category != null && !ordered.contains(category)) {
                ordered.add(category);
            }
        }
        for (Category category : categoryService.activeCategories()) {
            if (category != null && !ordered.contains(category)) {
                ordered.add(category);
            }
        }
        return ordered;
    }

    @Transactional
    public int backfillSubmitterRoles() {
        List<Ticket> missing = ticketRepository.findWithoutSubmitterRole();
        for (Ticket ticket : missing) {
            Role role = ticket.getRequester() == null ? null : ticket.getRequester().getRole();
            ticket.setSubmitterRole(role == null ? Role.STUDENT : role);
        }
        if (!missing.isEmpty()) {
            ticketRepository.saveAll(missing);
        }
        return missing.size();
    }

    public List<TicketComment> comments(Ticket ticket) {
        return commentService.list(ticket);
    }

    public List<TicketAttachment> attachments(Ticket ticket) {
        return attachmentRepository.findByTicketOrderByUploadedAtAsc(ticket);
    }

    /** CREATE: save a new open ticket and record who submitted it. */
    @Transactional
    public Ticket create(User requester, TicketForm form) {
        if (requester == null) {
            throw new IllegalArgumentException("Authentication required. Please sign in again.");
        }
        if (form == null || form.getCategoryId() == null || form.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Please select a department");
        }
        if (form.getTitle() == null || form.getTitle().trim().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (form.getDescription() == null || form.getDescription().trim().isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        Category category = categoryService.requireActive(form.getCategoryId());
        requireCanonicalCategory(category);
        // Build an open ticket for a student/lecturer or for staff.
        Ticket ticket = ticketFactory.create(requester, form, category);
        // Insert the new row.
        Ticket saved = ticketRepository.save(ticket);
        // Write "who created this" into the ticket history.
        auditService.log(requester, "Created ticket", "TICKET", saved.getId());
        if (saved.getStatus() != TicketStatus.OPEN) {
            recordStatusChange(requester, saved, TicketStatus.OPEN, saved.getStatus());
            fireStatusChanged(saved, TicketStatus.OPEN, saved.getStatus());
        }

        fireCreated(saved);
        if (saved.getAssignee() != null) {
            fireAssigned(saved, saved.getAssignee());
        }
        return saved;
    }

    /**
     * UPDATE: change title, description, or category.
     * A student or lecturer can do this only while the ticket is open and unassigned.
     */
    @Transactional
    public Ticket update(Long ticketId, User actor, TicketUpdateRequest request) {
        Ticket ticket = requireVisible(ticketId, actor);
        if (isSubmitter(actor)) {
            if (!owns(ticket, actor)) {
                throw new IllegalArgumentException("You cannot edit this ticket.");
            }
            // Owners can edit only before a staff member is assigned.
            if (!isOpenPhase(ticket)) {
                throw new IllegalArgumentException("You cannot edit a ticket after it has been assigned or moved to In Progress.");
            }
        } else if (isTerminal(ticket)) {
            // Staff cannot edit a resolved or closed ticket.
            throw new IllegalArgumentException("Cannot update a ticket after it has been resolved.");
        }
        boolean hasTitle = request.getTitle() != null && !request.getTitle().isBlank();
        boolean hasDescription = request.getDescription() != null && !request.getDescription().isBlank();
        boolean hasPriority = request.getPriority() != null;
        boolean hasCategory = request.getCategoryId() != null;
        if (!hasTitle && !hasDescription && !hasPriority && !hasCategory) {
            throw new IllegalArgumentException("Provide a title, description, priority, or category to update.");
        }
        if (hasTitle) {
            String title = request.getTitle().trim();
            if (title.length() > 200) {
                throw new IllegalArgumentException("Title must be at most 200 characters.");
            }
            ticket.setTitle(title); // save the new title
        }
        if (hasDescription) {
            String description = request.getDescription().trim();
            if (description.length() > 4000) {
                throw new IllegalArgumentException("Description must be at most 4000 characters.");
            }
            ticket.setDescription(description); // save the new description
        }
        if (hasPriority && !isSubmitter(actor) && ticket.getPriority() != request.getPriority()) {
            ticket.setPriority(request.getPriority());
            ticketFactory.applySla(ticket, ticket.getCategory());
        }
        if (hasCategory) {
            Category category = categoryService.requireActive(request.getCategoryId());
            requireCanonicalCategory(category);
            ticket.setCategory(category);
            ticketFactory.applySla(ticket, category);
        }
        Ticket saved = ticketRepository.save(ticket); // write the changes
        auditService.log(actor, "Updated ticket details", "TICKET", saved.getId());
        fireUpdated(saved);
        return saved;
    }

    /**
     * DELETE: remove the ticket and its files.
     * The owner can delete an open unassigned ticket, or any ticket after staff approve a delete request.
     * An administrator can delete a ticket in any status.
     */
    @Transactional
    public void delete(Long ticketId, User actor) {
        Ticket ticket = requireVisible(ticketId, actor);
        // Owner: open and unassigned, or after staff approve the delete request. Anyone else: administrator only.
        if (!canDeleteTicket(ticket, actor)) {
            if (isSubmitter(actor) && owns(ticket, actor)) {
                if (ticket.isDeleteRequested() && !ticket.isDeleteApproved()) {
                    throw new IllegalArgumentException("Staff have not approved this delete request yet.");
                }
                throw new IllegalArgumentException(
                        "Ask a staff member to approve deletion before you can delete this ticket."
                );
            }
            if (actor != null && actor.getRole() != Role.ADMIN && !isSubmitter(actor)) {
                throw new IllegalArgumentException(
                        "Only an administrator can delete a ticket after it has been assigned or closed."
                );
            }
            throw new IllegalArgumentException("You cannot delete this ticket.");
        }
        notificationService.detachTicket(ticket);
        auditService.log(actor, "Withdrew ticket", "TICKET", ticket.getId());
        // Capture files first. Cascade removes the rows, but it does not touch the disk.
        List<Path> storedFiles = new ArrayList<>();
        for (TicketAttachment attachment : attachmentRepository.findByTicketOrderByUploadedAtAsc(ticket)) {
            try {
                storedFiles.add(resolveStoredFile(attachment.getFilePath()));
            } catch (RuntimeException ex) {
                CLEANUP_LOG.error("Failed to resolve attachment file path={} reason={}",
                        attachment.getFilePath(), ex.getMessage());
            }
        }
        ticketRepository.delete(ticket); // remove the ticket row
        for (Path storedFile : storedFiles) {
            scheduleFileDelete(storedFile);
        }
    }

    @Transactional
    public Ticket requestDelete(Long ticketId, User actor) {
        Ticket ticket = requireVisible(ticketId, actor);
        if (!isSubmitter(actor) || !owns(ticket, actor)) {
            throw new IllegalArgumentException("You cannot request deletion of this ticket.");
        }
        if (isOpenPhase(ticket)) {
            throw new IllegalArgumentException("You can delete this ticket directly while it is open and unassigned.");
        }
        ticket.setDeleteRequested(true);
        Ticket saved = ticketRepository.save(ticket);
        auditService.log(actor, "Requested ticket deletion", "TICKET", saved.getId());
        return saved;
    }

    @Transactional
    public Ticket approveDelete(Long ticketId, User actor) {
        if (!isHelpDeskStaff(actor)) {
            throw new IllegalArgumentException("You cannot approve ticket deletion.");
        }
        Ticket ticket = requireVisible(ticketId, actor);
        if (!ticket.isDeleteRequested()) {
            throw new IllegalArgumentException("There is no delete request to approve.");
        }
        ticket.setDeleteApproved(true);
        Ticket saved = ticketRepository.save(ticket);
        auditService.log(actor, "Approved ticket deletion", "TICKET", saved.getId());
        if (saved.getRequester() != null) {
            notificationService.notify(
                    saved.getRequester(),
                    "Staff approved deletion of " + saved.getTicketNumber() + ". You can delete it now.",
                    saved
            );
        }
        return saved;
    }

    /** Owner accepts a resolved ticket and closes it. The assignee stays the same. */
    @Transactional
    public Ticket confirmClose(Long ticketId, User actor) {
        Ticket ticket = requireVisible(ticketId, actor);
        if (!canConfirmClose(ticket, actor)) {
            throw new IllegalArgumentException("Only the person who opened a resolved ticket can confirm and close it.");
        }
        return closeResolved(ticket, actor, "requester confirmed", null);
    }

    /** Owner says the fix did not work. Status goes back to In Progress with the same assignee. */
    @Transactional
    public Ticket reopenResolved(Long ticketId, User actor) {
        Ticket ticket = requireVisible(ticketId, actor);
        if (!canReopenResolved(ticket, actor)) {
            throw new IllegalArgumentException("Only the person who opened this ticket can reopen it while it is resolved.");
        }
        User assignee = ticket.getAssignee();
        TicketStatus previous = ticket.getStatus();
        ticket.setAssignee(assignee);
        ticket.setStatus(assignee != null ? TicketStatus.IN_PROGRESS : TicketStatus.OPEN);
        ticket.setResolvedAt(null);
        Ticket saved = ticketRepository.save(ticket);
        auditService.log(actor, "Reopened resolved ticket. Assignee kept.", "TICKET", saved.getId());
        recordStatusChange(actor, saved, previous, saved.getStatus());
        fireStatusChanged(saved, previous, saved.getStatus());
        return saved;
    }

    /** Staff close a resolved ticket and must leave a short note. */
    @Transactional
    public Ticket staffCloseResolved(Long ticketId, User actor, String reason) {
        String note = requireNote(reason);
        Ticket ticket = requireVisible(ticketId, actor);
        if (!canStaffClose(ticket, actor)) {
            throw new IllegalArgumentException("Staff can close a ticket this way only while it is resolved.");
        }
        return closeResolved(ticket, actor, "staff override", note);
    }

    /**
     * Closes resolved tickets that the requester has not confirmed or reopened for five days.
     * Returns how many tickets were closed.
     */
    @Transactional
    public int autoCloseResolvedTickets() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RESOLVED_RESPONSE_DAYS);
        List<Ticket> stale = ticketRepository.findByStatusAndResolvedAtLessThanEqual(TicketStatus.RESOLVED, cutoff);
        int closed = 0;
        for (Ticket ticket : stale) {
            closeResolved(ticket, null, "auto-closed", null);
            closed++;
        }
        return closed;
    }

    private Ticket closeResolved(Ticket ticket, User actor, String how, String reason) {
        User assignee = ticket.getAssignee();
        TicketStatus previous = ticket.getStatus();
        ticket.setAssignee(assignee);
        ticket.setStatus(TicketStatus.CLOSED);
        if (ticket.getResolvedAt() == null) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        Ticket saved = ticketRepository.save(ticket);
        String action = "Closed ticket. How: " + how;
        if (reason != null && !reason.isBlank()) {
            action = action + ". Reason: " + reason;
        }
        auditService.log(actor, trimAction(action), "TICKET", saved.getId());
        if (previous != TicketStatus.CLOSED) {
            recordStatusChange(actor, saved, previous, TicketStatus.CLOSED);
            fireStatusChanged(saved, previous, TicketStatus.CLOSED);
        }
        return saved;
    }

    private String requireNote(String reason) {
        String note = reason == null ? "" : reason.trim();
        if (note.isBlank()) {
            throw new IllegalArgumentException("A reason is required to close this ticket.");
        }
        if (note.length() > 500) {
            throw new IllegalArgumentException("Reason must be at most 500 characters.");
        }
        return note;
    }

    private String trimAction(String action) {
        if (action.length() > 200) {
            return action.substring(0, 200);
        }
        return action;
    }

    @Transactional
    public Ticket reopen(Long ticketId, User actor, String reason) {
        String note = reason == null ? "" : reason.trim();
        if (note.isBlank()) {
            throw new IllegalArgumentException("A reason is required to reopen this ticket.");
        }
        if (note.length() > 500) {
            throw new IllegalArgumentException("Reason must be at most 500 characters.");
        }
        Ticket ticket = requireVisible(ticketId, actor);
        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new IllegalArgumentException("Only CLOSED tickets can be reopened.");
        }
        if (!canReopenTicket(ticket, actor)) {
            throw new IllegalArgumentException(CLOSED_FOLLOW_UP);
        }
        TicketStatus previous = ticket.getStatus();
        User assignee = ticket.getAssignee();
        ticket.setStatus(assignee != null ? TicketStatus.IN_PROGRESS : TicketStatus.OPEN);
        ticket.setResolvedAt(null);
        Ticket saved = ticketRepository.save(ticket);
        String action = "Reopened ticket. Reason: " + note;
        if (action.length() > 200) {
            action = action.substring(0, 200);
        }
        auditService.log(actor, action, "TICKET", saved.getId());
        recordStatusChange(actor, saved, previous, saved.getStatus());
        fireStatusChanged(saved, previous, saved.getStatus());
        return saved;
    }

    @Transactional
    public TicketAttachment addAttachment(Long ticketId, User actor, MultipartFile file) {
        Ticket ticket = requireVisible(ticketId, actor);
        if (isSubmitter(actor) && !isOpenPhase(ticket)) {
            throw new IllegalArgumentException("You can only attach files while the ticket is open and unassigned.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }
        if (file.getSize() > getMaxAttachmentBytes()) {
            throw new IllegalArgumentException("File exceeds the " + maxAttachmentMb + " MB limit.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only PNG, JPEG, GIF, WebP, or PDF files are allowed.");
        }
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename());
        if (originalName.contains("..") || originalName.isBlank()) {
            throw new IllegalArgumentException("Invalid file name.");
        }
        String storedName = UUID.randomUUID() + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path ticketDir = uploadRoot.resolve(String.valueOf(ticket.getId()));
        Path destination = ticketDir.resolve(storedName).normalize();
        if (!destination.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid file path.");
        }
        try {
            Files.createDirectories(ticketDir);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not store the attachment: " + ex.getMessage());
        }

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicket(ticket);
        attachment.setFileName(originalName);
        attachment.setFilePath("/uploads/" + ticket.getId() + "/" + storedName);
        TicketAttachment saved = attachmentRepository.save(attachment);
        auditService.log(actor, "Uploaded attachment " + originalName, "TICKET", ticket.getId());
        return saved;
    }

    public TicketAttachment requireAttachment(Long ticketId, Long attachmentId, User actor) {
        Ticket ticket = requireVisible(ticketId, actor);
        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        if (attachment.getTicket() == null || !attachment.getTicket().getId().equals(ticket.getId())) {
            throw new IllegalArgumentException("Attachment not found");
        }
        return attachment;
    }

    @Transactional
    public void removeAttachment(Long ticketId, Long attachmentId, User actor) {
        TicketAttachment attachment = requireAttachment(ticketId, attachmentId, actor);
        if (isSubmitter(actor) && (attachment.getTicket() == null || !isOpenPhase(attachment.getTicket()))) {
            throw new IllegalArgumentException("You can only remove files while the ticket is open and unassigned.");
        }
        // Resolve before the row is removed so a bad path never deletes the database record.
        Path storedFile = resolveStoredFile(attachment.getFilePath());
        attachmentRepository.delete(attachment);
        auditService.log(actor, "Deleted attachment " + attachment.getFileName(), "TICKET", ticketId);
        // File removal waits until the database delete has committed.
        scheduleFileDelete(storedFile);
    }

    /**
     * Attachment rows whose file is no longer on disk. Used to find leftovers from the old delete bug.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> attachmentsMissingOnDisk() {
        List<Map<String, Object>> missing = new ArrayList<>();
        for (TicketAttachment attachment : attachmentRepository.findAll()) {
            boolean absent;
            try {
                absent = !Files.exists(resolveStoredFile(attachment.getFilePath()));
            } catch (RuntimeException ex) {
                absent = true;
            }
            if (absent) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", attachment.getId());
                row.put("ticketId", attachment.getTicket() == null ? null : attachment.getTicket().getId());
                row.put("fileName", attachment.getFileName());
                row.put("filePath", attachment.getFilePath());
                missing.add(row);
            }
        }
        return missing;
    }

    /**
     * Files sitting in the upload folder that no attachment row still points at.
     */
    public List<String> orphanUploadFiles() {
        Set<String> referenced = new java.util.HashSet<>();
        for (TicketAttachment attachment : attachmentRepository.findAll()) {
            try {
                referenced.add(resolveStoredFile(attachment.getFilePath()).toAbsolutePath().normalize().toString());
            } catch (RuntimeException ignored) {
                // Unresolvable paths are reported by attachmentsMissingOnDisk instead.
            }
        }
        List<String> orphans = new ArrayList<>();
        if (!Files.isDirectory(uploadRoot)) {
            return orphans;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(uploadRoot)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                Path absolute = file.toAbsolutePath().normalize();
                String fileName = absolute.getFileName() == null ? "" : absolute.getFileName().toString();
                // Keep folder placeholders such as .gitkeep. They are not leftover uploads.
                if (fileName.startsWith(".")) {
                    return;
                }
                if (!referenced.contains(absolute.toString())) {
                    orphans.add(uploadRoot.relativize(absolute).toString().replace('\\', '/'));
                }
            });
        } catch (IOException ex) {
            CLEANUP_LOG.error("Failed to scan upload directory path={} reason={}", uploadRoot, ex.getMessage());
        }
        return orphans;
    }

    /**
     * Deletes only files that are not referenced by a ticket_attachments row.
     */
    public int deleteOrphanUploadFiles() {
        int removed = 0;
        for (String relative : orphanUploadFiles()) {
            try {
                Path file = resolveStoredFile("/uploads/" + relative);
                if (Files.deleteIfExists(file)) {
                    removed++;
                }
            } catch (RuntimeException | IOException ex) {
                CLEANUP_LOG.error("Failed to delete orphan attachment file path={} reason={}", relative, ex.getMessage());
            }
        }
        return removed;
    }

    /**
     * Turns the stored web path into a file under the upload root, and rejects traversal or absolute paths.
     */
    public Path resolveStoredFile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new IllegalArgumentException("Invalid attachment path.");
        }
        String normalized = storedPath.replace('\\', '/').trim();
        if (normalized.contains("..") || normalized.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid attachment path.");
        }
        if (normalized.matches("^[A-Za-z]:/.*") || normalized.startsWith("//")) {
            throw new IllegalArgumentException("Invalid attachment path.");
        }
        String relative;
        if (normalized.startsWith("/uploads/")) {
            relative = normalized.substring("/uploads/".length());
        } else if (normalized.startsWith("uploads/")) {
            relative = normalized.substring("uploads/".length());
        } else if (normalized.startsWith("/")) {
            throw new IllegalArgumentException("Invalid attachment path.");
        } else {
            relative = normalized;
        }
        if (relative.isBlank()) {
            throw new IllegalArgumentException("Invalid attachment path.");
        }
        Path root = uploadRoot.toAbsolutePath().normalize();
        Path file = root.resolve(relative).toAbsolutePath().normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Invalid attachment path.");
        }
        return file;
    }

    /**
     * Deletes the physical file after the surrounding transaction commits.
     * A disk failure is logged and does not put the database row back.
     */
    private void scheduleFileDelete(Path file) {
        Runnable delete = () -> {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ex) {
                CLEANUP_LOG.error("Failed to delete attachment file path={} reason={}", file, ex.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delete.run();
                }
            });
            return;
        }
        delete.run();
    }

    @Transactional
    public void addComment(Long ticketId, User author, String body) {
        commentService.add(ticketId, author, body);
    }

    @Transactional
    public void updateStatus(Long ticketId, User actor, TicketStatus status) {
        Ticket ticket = requireVisible(ticketId, actor);
        if (isSubmitter(actor)) {
            if (!owns(ticket, actor)) {
                throw new IllegalArgumentException("You cannot change this ticket.");
            }
            if (!isOpenPhase(ticket)) {
                throw new IllegalArgumentException("You can only add comments after this ticket is assigned.");
            }
            if (status != TicketStatus.CLOSED) {
                throw new IllegalArgumentException(
                        "You cannot set this status. Students and lecturers can only close or reopen a ticket."
                );
            }
        }
        if (status == TicketStatus.IN_PROGRESS && ticket.getAssignee() == null) {
            throw new IllegalArgumentException("A ticket cannot be In Progress without an assignee.");
        }
        TicketStatus previous = ticket.getStatus();
        if (isHelpDeskStaff(actor) && previous == TicketStatus.RESOLVED && status == TicketStatus.CLOSED) {
            throw new IllegalArgumentException("Add a short reason to close a resolved ticket.");
        }
        ticket.setStatus(status);
        if ((status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) && ticket.getResolvedAt() == null) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (status == TicketStatus.OPEN || status == TicketStatus.IN_PROGRESS) {
            ticket.setResolvedAt(null);
        }
        ticketRepository.save(ticket);
        if (previous != status) {
            recordStatusChange(actor, ticket, previous, status);
            fireStatusChanged(ticket, previous, status);
        }
    }

    @Transactional
    public Ticket reassign(Long ticketId, User actor, Long assigneeId) {
        if (!isHelpDeskStaff(actor)) {
            throw new IllegalArgumentException("Only staff can reassign tickets.");
        }
        Ticket ticket = requireVisible(ticketId, actor);
        User assignee = authService.staffMembers().stream()
                .filter(u -> u.getId().equals(assigneeId))
                .filter(this::isHelpDeskStaff)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Assignee must be staff."));
        ticket.setAssignee(assignee);
        TicketStatus previous = ticket.getStatus();
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        if (previous == TicketStatus.RESOLVED || previous == TicketStatus.CLOSED) {
            ticket.setResolvedAt(null);
        }
        Ticket saved = ticketRepository.save(ticket);
        if (previous != saved.getStatus()) {
            recordStatusChange(actor, saved, previous, saved.getStatus());
            fireStatusChanged(saved, previous, saved.getStatus());
        }
        auditService.log(actor, "Assigned ticket to " + assignee.getFullName(), "TICKET", saved.getId());
        fireAssigned(saved, assignee);
        return saved;
    }

    @Transactional
    public Ticket escalate(Long ticketId, User actor) {
        if (!isHelpDeskStaff(actor)) {
            throw new IllegalArgumentException("Only staff can escalate tickets.");
        }
        Ticket ticket = requireVisible(ticketId, actor);
        TicketPriority previousPriority = ticket.getPriority() == null ? TicketPriority.MEDIUM : ticket.getPriority();
        ticket.setPriority(previousPriority.escalate());
        ticketFactory.applySla(ticket, ticket.getCategory());

        User senior = findSeniorAssignee(ticket);
        TicketStatus previousStatus = ticket.getStatus();
        if (senior != null) {
            ticket.setAssignee(senior);
            if (ticket.getStatus() == TicketStatus.OPEN) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
            }
        }
        Ticket saved = ticketRepository.save(ticket);
        auditService.log(
                actor,
                "Escalated ticket to " + saved.getPriority().name()
                        + (senior == null ? "" : " and assigned to " + senior.getFullName()),
                "TICKET",
                saved.getId()
        );
        if (previousStatus != saved.getStatus()) {
            recordStatusChange(actor, saved, previousStatus, saved.getStatus());
            fireStatusChanged(saved, previousStatus, saved.getStatus());
        }
        if (senior != null) {
            fireAssigned(saved, senior);
        }
        notificationService.notify(
                saved.getRequester(),
                "Ticket " + saved.getTicketNumber() + " was escalated to " + saved.getPriority().name(),
                saved
        );
        return saved;
    }

    public List<com.sliit.helpdesk.report.model.AuditLog> history(Long ticketId, User actor) {
        Ticket ticket = requireVisible(ticketId, actor);
        return auditService.forEntity("TICKET", ticket.getId());
    }

    @Transactional
    public TicketComment addFeedback(Long ticketId, User actor, int rating, String comment) {
        Ticket ticket = requireVisible(ticketId, actor);
        if (ticket.getRequester() == null || !ticket.getRequester().getId().equals(actor.getId())) {
            throw new IllegalArgumentException("Only the requester can submit feedback.");
        }
        if (ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new IllegalArgumentException("Feedback can only be submitted after a ticket is resolved.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        String note = comment == null || comment.isBlank()
                ? "Feedback: " + rating + "/5"
                : "Feedback: " + rating + "/5 — " + comment.trim();
        auditService.log(actor, "Submitted feedback " + rating + "/5", "TICKET", ticket.getId());
        return commentService.add(ticketId, actor, note);
    }

    private User findSeniorAssignee(Ticket ticket) {
        List<User> staff = authService.staffMembers().stream().filter(User::isEnabled).toList();
        String department = ticket.getCategory() == null ? null : ticket.getCategory().getDepartment();
        return staff.stream()
                .filter(user -> user.getRole() == Role.DEPT_HEAD)
                .filter(user -> department == null || department.isBlank()
                        || department.equalsIgnoreCase(user.getDepartment()))
                .findFirst()
                .or(() -> staff.stream().filter(user -> user.getRole() == Role.DEPT_HEAD).findFirst())
                .or(() -> staff.stream().filter(user -> user.getRole() == Role.ADMIN).findFirst())
                .orElse(null);
    }

    private boolean owns(Ticket ticket, User actor) {
        return ticket.getRequester() != null
                && actor != null
                && ticket.getRequester().getId().equals(actor.getId());
    }

    private void requireCanonicalCategory(Category category) {
        if (category == null || category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("Please select a valid category.");
        }
    }

    private void recordStatusChange(User actor, Ticket ticket, TicketStatus from, TicketStatus to) {
        auditService.log(
                actor,
                "Updated ticket status from " + from.name() + " to " + to.name(),
                "TICKET",
                ticket.getId()
        );
    }

    private void fireCreated(Ticket ticket) {
        ticketEventListeners.forEach(listener -> listener.onCreated(ticket));
    }

    private void fireUpdated(Ticket ticket) {
        ticketEventListeners.forEach(listener -> listener.onUpdated(ticket));
    }

    private void fireStatusChanged(Ticket ticket, TicketStatus from, TicketStatus to) {
        ticketEventListeners.forEach(listener -> listener.onStatusChanged(ticket, from, to));
    }

    private void fireAssigned(Ticket ticket, User assignee) {
        ticketEventListeners.forEach(listener -> listener.onAssigned(ticket, assignee));
    }
}
