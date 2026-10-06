package com.sliit.helpdesk.ticket.repository;

// Ticket Attachment Repository is part of the campus help desk repository code.

import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    List<TicketAttachment> findByTicketOrderByUploadedAtAsc(Ticket ticket);
}
