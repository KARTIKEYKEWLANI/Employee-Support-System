package com.support.ticketing.dto.queue;

import java.time.Instant;

public record QueueResponse(
        Long id,
        String name,
        String description,
        boolean autoAssign,
        Instant createdAt
) {
}
