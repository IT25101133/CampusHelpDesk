package com.sliit.helpdesk.report.service;

import com.sliit.helpdesk.auth.dto.ProfileUpdateRequest;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.ticket.model.Ticket;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Records account and ticket actions. The password argument is never read into the log.
 */
@Aspect
@Component
public class ActivityAuditAspect {

    private final AuditEventService auditEventService;

    public ActivityAuditAspect(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @AfterReturning("execution(* com.sliit.helpdesk.auth.service.UserService.login(..))")
    public void loginSuccess(JoinPoint joinPoint) {
        safely(() -> auditEventService.recordByEmail(emailArg(joinPoint), "LOGIN_SUCCESS", "USER", null, null));
    }

    @AfterThrowing("execution(* com.sliit.helpdesk.auth.service.UserService.login(..))")
    public void loginFailure(JoinPoint joinPoint) {
        safely(() -> auditEventService.recordByEmail(emailArg(joinPoint), "LOGIN_FAILURE", "USER", null, null));
    }

    @AfterReturning("execution(* com.sliit.helpdesk.auth.controller.AuthApiController.logout(..))")
    public void logout(JoinPoint joinPoint) {
        Object auth = joinPoint.getArgs().length == 0 ? null : joinPoint.getArgs()[0];
        if (auth instanceof org.springframework.security.core.Authentication authentication) {
            safely(() -> auditEventService.recordByEmail(authentication.getName(), "LOGOUT", "USER", null, null));
        }
    }

    @AfterReturning(pointcut = "execution(* com.sliit.helpdesk.auth.service.UserService.updateProfile(..))", returning = "user")
    public void profileUpdated(JoinPoint joinPoint, User user) {
        String action = "PROFILE_UPDATE";
        if (joinPoint.getArgs().length > 1 && joinPoint.getArgs()[1] instanceof ProfileUpdateRequest request
                && request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            action = "PASSWORD_CHANGE";
        }
        String recorded = action;
        safely(() -> auditEventService.record(user, recorded, "USER", user == null ? null : user.getId(), null));
    }

    @AfterReturning("execution(* com.sliit.helpdesk.auth.service.UserService.resetPassword(..))")
    public void passwordReset(JoinPoint joinPoint) {
        safely(() -> auditEventService.recordByEmail(emailArg(joinPoint), "PASSWORD_CHANGE", "USER", null, null));
    }

    @AfterReturning(pointcut = "execution(* com.sliit.helpdesk.auth.service.UserService.deactivateAccount(..))", returning = "user")
    public void deactivated(User user) {
        safely(() -> auditEventService.record(user, "ACCOUNT_DEACTIVATE", "USER", user == null ? null : user.getId(), null));
    }

    @AfterReturning(pointcut = "execution(* com.sliit.helpdesk.auth.service.UserService.update(..))", returning = "user")
    public void roleChanged(User user) {
        String role = user == null || user.getRole() == null ? null : user.getRole().name();
        safely(() -> auditEventService.record(user, "ROLE_CHANGE", "USER", user == null ? null : user.getId(),
                role == null ? null : "role=" + role));
    }

    @AfterReturning(pointcut = "execution(* com.sliit.helpdesk.ticket.service.TicketService.create(..))", returning = "ticket")
    public void ticketCreated(JoinPoint joinPoint, Ticket ticket) {
        safely(() -> auditEventService.record(actor(joinPoint), "TICKET_CREATE", "TICKET",
                ticket == null ? null : ticket.getId(), null));
    }

    @AfterReturning(pointcut = "execution(* com.sliit.helpdesk.ticket.service.TicketService.update(..))", returning = "ticket")
    public void ticketUpdated(JoinPoint joinPoint, Ticket ticket) {
        safely(() -> auditEventService.record(actor(joinPoint), "TICKET_UPDATE", "TICKET",
                ticket == null ? null : ticket.getId(), null));
    }

    @AfterReturning("execution(* com.sliit.helpdesk.ticket.service.TicketService.delete(..))")
    public void ticketDeleted(JoinPoint joinPoint) {
        Long ticketId = joinPoint.getArgs().length == 0 ? null : (Long) joinPoint.getArgs()[0];
        User actor = joinPoint.getArgs().length > 1 && joinPoint.getArgs()[1] instanceof User user ? user : null;
        safely(() -> auditEventService.record(actor, "TICKET_DELETE", "TICKET", ticketId, null));
    }

    private static void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger(ActivityAuditAspect.class)
                    .warn("Activity history was not saved: {}", ex.getMessage());
        }
    }

    private static String emailArg(JoinPoint joinPoint) {
        if (joinPoint.getArgs().length == 0 || joinPoint.getArgs()[0] == null) {
            return null;
        }
        return String.valueOf(joinPoint.getArgs()[0]);
    }

    private static User actor(JoinPoint joinPoint) {
        if (joinPoint.getArgs().length == 0) {
            return null;
        }
        return joinPoint.getArgs()[0] instanceof User user ? user : null;
    }
}
