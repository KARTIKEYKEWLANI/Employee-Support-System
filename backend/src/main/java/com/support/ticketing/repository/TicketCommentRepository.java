package com.support.ticketing.repository;

import com.support.ticketing.entity.Ticket;
import com.support.ticketing.entity.TicketComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    List<TicketComment> findAllByTicketOrderByCreatedAtAsc(Ticket ticket);
}
