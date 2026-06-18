package com.support.ticketing.repository;

import com.support.ticketing.entity.Ticket;
import com.support.ticketing.entity.TicketRating;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRatingRepository extends JpaRepository<TicketRating, Long> {
    Optional<TicketRating> findByTicket(Ticket ticket);
}
