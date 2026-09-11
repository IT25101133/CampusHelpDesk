package com.sliit.helpdesk.ticket.repository;

import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    List<TicketAttachment> findByTicketOrderByUploadedAtAsc(Ticket ticket);
}
