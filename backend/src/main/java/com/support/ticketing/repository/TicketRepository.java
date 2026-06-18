package com.support.ticketing.repository;

import com.support.ticketing.entity.Ticket;
import com.support.ticketing.entity.TicketStatus;
import com.support.ticketing.entity.SupportQueue;
import com.support.ticketing.entity.User;
import java.util.List;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    Page<Ticket> findAllByUser(User user, Pageable pageable);

    List<Ticket> findAllByUser(User user);

    long countByAssigneesContainingAndStatusIn(User user, Collection<TicketStatus> statuses);

    long countByAssigneesContainingAndQueueAndStatusIn(User user, SupportQueue queue, Collection<TicketStatus> statuses);

    List<Ticket> findAllByAssignedTo(User user);

    List<Ticket> findAllByAssigneesContaining(User user);
}
