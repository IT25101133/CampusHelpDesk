package com.sliit.helpdesk.notification;

// Notification Decorator Test is part of the campus help desk notification code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.notification.decorator.EmailNotificationDecorator;
import com.sliit.helpdesk.notification.decorator.InAppNotificationSender;
import com.sliit.helpdesk.notification.decorator.NotificationSender;
import com.sliit.helpdesk.notification.decorator.UrgentPriorityNotificationDecorator;
import com.sliit.helpdesk.notification.model.Notification;
import com.sliit.helpdesk.notification.repository.NotificationRepository;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationDecoratorTest {

    @Mock
    private NotificationRepository notificationRepository;

    private User recipient;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        recipient = new User();
        recipient.setId(10L);
        recipient.setEmail("student@sliit.lk");
        recipient.setFullName("Student User");
        recipient.setRole(Role.STUDENT);

        ticket = new Ticket();
        ticket.setId(42L);
        ticket.setTitle("Library Wi-Fi down");
        ticket.setPriority(TicketPriority.CRITICAL);
    }

    @Test
    void inAppSenderSavesNotification() {
        NotificationSender inAppSender = new InAppNotificationSender(notificationRepository);
        inAppSender.send(recipient, "Test notification", ticket);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getRecipient()).isEqualTo(recipient);
        assertThat(saved.getMessage()).isEqualTo("Test notification");
        assertThat(saved.getTicket()).isEqualTo(ticket);
    }

    @Test
    void emailDecoratorWrapsInAppSenderAndPreservesDelegation() {
        NotificationSender inAppSender = new InAppNotificationSender(notificationRepository);
        NotificationSender emailDecorator = new EmailNotificationDecorator(inAppSender);

        emailDecorator.send(recipient, "Status changed to IN_PROGRESS", ticket);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("Status changed to IN_PROGRESS");
    }

    @Test
    void urgentDecoratorPrependsUrgentTagForCriticalTickets() {
        NotificationSender inAppSender = new InAppNotificationSender(notificationRepository);
        NotificationSender urgentDecorator = new UrgentPriorityNotificationDecorator(inAppSender);

        urgentDecorator.send(recipient, "Network outage", ticket);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("[URGENT] Network outage");
    }

    @Test
    void stackedDecoratorsComposeBehaviorsCorrectly() {
        NotificationSender inAppSender = new InAppNotificationSender(notificationRepository);
        // Compose: UrgentDecorator -> EmailDecorator -> InAppSender
        NotificationSender pipeline = new UrgentPriorityNotificationDecorator(new EmailNotificationDecorator(inAppSender));

        pipeline.send(recipient, "System server restart", ticket);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("[URGENT] System server restart");
    }
}
