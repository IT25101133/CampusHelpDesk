package com.sliit.helpdesk.notification;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.notification.model.Notification;
import com.sliit.helpdesk.notification.repository.NotificationRepository;
import com.sliit.helpdesk.notification.service.NotificationService;
import com.sliit.helpdesk.ticket.model.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void notifyPersistsUnreadAssignment() {
        User staff = new User();
        staff.setId(2L);
        Ticket ticket = new Ticket();
        ticket.setId(5L);

        notificationService.notify(staff, "Ticket assigned to you: HD-5", ticket);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo(staff);
        assertThat(captor.getValue().getMessage()).isEqualTo("Ticket assigned to you: HD-5");
        assertThat(captor.getValue().isRead()).isFalse();
        assertThat(captor.getValue().getTicket()).isEqualTo(ticket);
    }
}
