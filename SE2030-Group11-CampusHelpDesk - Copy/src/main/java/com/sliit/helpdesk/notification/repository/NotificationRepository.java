package com.sliit.helpdesk.notification.repository;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndReadFalse(User recipient);
}
