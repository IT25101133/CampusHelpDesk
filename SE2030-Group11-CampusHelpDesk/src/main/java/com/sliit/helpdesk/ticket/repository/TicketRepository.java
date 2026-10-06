package com.sliit.helpdesk.ticket.repository;

// Ticket Repository is part of the campus help desk repository code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.ticket.model.Ticket;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @Query("select t from Ticket t join fetch t.requester where t.submitterRole is null")
    List<Ticket> findWithoutSubmitterRole();

    List<Ticket> findByRequesterOrderByCreatedAtDesc(User requester);

    List<Ticket> findByAssigneeOrderByCreatedAtDesc(User assignee);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    long countByStatus(TicketStatus status);

    List<Ticket> findByStatusAndResolvedAtLessThanEqual(TicketStatus status, LocalDateTime resolvedAt);

    long countByCategoryId(Long categoryId);

    long countByRequester_Id(Long requesterId);

    long countByAssignee_Id(Long assigneeId);

    @Query("select t.status, count(t) from Ticket t group by t.status")
    List<Object[]> countGroupedByStatus();

    @Query("select t.priority, count(t) from Ticket t group by t.priority")
    List<Object[]> countGroupedByPriority();

    @Query("select c.name, count(t) from Ticket t join t.category c group by c.name")
    List<Object[]> countGroupedByCategory();

    @Query("select c.id, c.name, count(t) from Ticket t join t.category c group by c.id, c.name order by c.name")
    List<Object[]> countGroupedByCategoryWithId();

    @Query("select distinct t from Ticket t left join fetch t.category left join fetch t.assignee")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Ticket> findAllForReporting();

    @Query("select distinct t from Ticket t left join fetch t.category left join fetch t.assignee"
            + " left join fetch t.requester order by t.createdAt desc")
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    List<Ticket> findAllForFiltering();
}
