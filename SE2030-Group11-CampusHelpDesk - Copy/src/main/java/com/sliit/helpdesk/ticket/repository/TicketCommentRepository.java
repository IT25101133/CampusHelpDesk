package com.sliit.helpdesk.ticket.repository;

import com.sliit.helpdesk.notification.model.TicketComment;
import com.sliit.helpdesk.ticket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    List<TicketComment> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}
