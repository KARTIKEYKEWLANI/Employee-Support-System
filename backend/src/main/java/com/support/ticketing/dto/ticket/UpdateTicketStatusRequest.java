package com.support.ticketing.dto.ticket;

import com.support.ticketing.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(@NotNull TicketStatus status) {
}
