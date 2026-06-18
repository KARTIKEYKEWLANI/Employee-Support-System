package com.support.ticketing.dto.ticket;

import java.time.Instant;

public record TicketAttachmentResponse(
        Long id,
        String filename,
        String contentType,
        long size,
        Instant createdAt
) {
}
