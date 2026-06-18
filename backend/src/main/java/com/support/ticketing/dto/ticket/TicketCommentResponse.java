package com.support.ticketing.dto.ticket;

import com.support.ticketing.entity.CommentType;
import java.time.Instant;

public record TicketCommentResponse(
        Long id,
        Long authorId,
        String authorName,
        String message,
        CommentType type,
        boolean internalNote,
        Instant createdAt
) {
}
