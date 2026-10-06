package com.sliit.helpdesk.notification.service;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.notification.model.Notification;
import com.sliit.helpdesk.notification.repository.NotificationRepository;
import com.sliit.helpdesk.ticket.model.Ticket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notify(User recipient, String message, Ticket ticket) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(trimMessage(message));
        notification.setTicket(ticket);
        notificationRepository.save(notification);
    }

    public List<Notification> inbox(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    public long unreadCount(User user) {
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    @Transactional
    public Notification markRead(Long id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot update this notification.");
        }
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(User user) {
        inbox(user).forEach(n -> n.setRead(true));
    }

    private static String trimMessage(String message) {
        if (message == null) {
            return "";
        }
        String trimmed = message.trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300);
    }
}
