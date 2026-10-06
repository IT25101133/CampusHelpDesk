package com.sliit.helpdesk.ticket.repository;

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByRequesterOrderByCreatedAtDesc(User requester);

    List<Ticket> findByAssigneeOrderByCreatedAtDesc(User assignee);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    long countByStatus(TicketStatus status);

    @Query("select t.status, count(t) from Ticket t group by t.status")
    List<Object[]> countGroupedByStatus();

    @Query("select t.priority, count(t) from Ticket t group by t.priority")
    List<Object[]> countGroupedByPriority();

    @Query("select c.name, count(t) from Ticket t join t.category c group by c.name")
    List<Object[]> countGroupedByCategory();
}
