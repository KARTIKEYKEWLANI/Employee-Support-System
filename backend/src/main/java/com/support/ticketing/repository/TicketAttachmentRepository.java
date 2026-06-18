package com.support.ticketing.repository;

import com.support.ticketing.entity.Ticket;
import com.support.ticketing.entity.TicketAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    List<TicketAttachment> findAllByTicket(Ticket ticket);
}
