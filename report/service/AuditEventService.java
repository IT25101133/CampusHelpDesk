package com.sliit.helpdesk.report.service;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.report.model.AuditEvent;
import com.sliit.helpdesk.report.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes and reads profile activity. Metadata is action context only — never a password.
 */
@Service
public class AuditEventService {

    public static final int PAGE_SIZE = 50;

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;

    public AuditEventService(AuditEventRepository auditEventRepository, UserRepository userRepository) {
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordByEmail(String email, String action, String targetEntity, Long targetId, String metadata) {
        User user = email == null || email.isBlank()
                ? null
                : userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        // Login and logout only know the email. Store that account's id as the target.
        Long resolvedTarget = targetId;
        if (resolvedTarget == null && user != null && user.getId() != null && "USER".equals(targetEntity)) {
            resolvedTarget = user.getId();
        }
        record(user, action, targetEntity, resolvedTarget, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User user, String action, String targetEntity, Long targetId, String metadata) {
        User managed = user == null || user.getId() == null
                ? null
                : userRepository.findById(user.getId()).orElse(null);
        AuditEvent event = new AuditEvent();
        event.setUser(managed);
        event.setAction(trim(action, 80));
        event.setTargetEntity(trim(targetEntity, 50));
        event.setTargetId(targetId);
        event.setMetadata(safeMetadata(metadata));
        HttpServletRequest request = currentRequest();
        if (request != null) {
            event.setIpAddress(trim(clientIp(request), 64));
            event.setUserAgent(trim(request.getHeader("User-Agent"), 255));
        }
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> page(User subject, String action, LocalDate from, LocalDate to, int page) {
        int safePage = Math.max(page, 0);
        String actionFilter = action == null || action.isBlank() ? null : action.trim();
        LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toTime = to == null ? null : to.atTime(LocalTime.MAX);
        Page<AuditEvent> result = auditEventRepository.search(
                subject.getId(),
                actionFilter,
                fromTime,
                toTime,
                PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<Map<String, Object>> items = result.getContent().stream().map(this::toItem).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("page", safePage);
        body.put("hasMore", result.hasNext());
        return body;
    }

    private Map<String, Object> toItem(AuditEvent event) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", event.getId());
        item.put("action", event.getAction());
        item.put("entityType", event.getTargetEntity());
        item.put("entityId", event.getTargetId());
        item.put("metadata", event.getMetadata());
        item.put("ipAddress", event.getIpAddress());
        item.put("createdAt", event.getCreatedAt());
        return item;
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String safeMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        String lower = metadata.toLowerCase();
        if (lower.contains("password")) {
            return null;
        }
        return trim(metadata, 500);
    }

    private static String trim(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
