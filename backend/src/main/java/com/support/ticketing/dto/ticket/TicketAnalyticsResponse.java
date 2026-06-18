package com.support.ticketing.dto.ticket;

public record TicketAnalyticsResponse(
        long totalTickets,
        long openTickets,
        long inProgressTickets,
        long resolvedTickets
) {
}
