package com.support.ticketing.dto.ticket;

import java.time.Instant;

public record TicketRatingResponse(
        Long id,
        int score,
        String feedback,
        Instant createdAt
) {
}
